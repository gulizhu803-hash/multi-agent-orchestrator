package org.example.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.service.armory.AbstractArmorySupport;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChatModelNode extends AbstractArmorySupport {
    @Resource
    private AgentWorkflowNode agentNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
//        流程：解析配置 -> 建立 MCP 同步客户端 (SSE/Stdio) -> 初始化工具列表 -> 注入 ChatModel。

        // 1. 获取上游已初始化的 OpenAI API 客户端
        // 原理：OpenAiApi 封装了 HTTP 通信细节（Retry, Timeout, Auth），此处复用以避免重复创建连接资源
        OpenAiApi openAiApi = dynamicContext.getOpenAiApi();

        // 2. 提取当前 Agent 的聊天模型配置
        // 结构：ArmoryCommand -> ConfigTable -> Module -> ChatModel
        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.ChatModel chatModelConfig = aiAgentConfigTableVO.getModule().getChatModel();

        // 3. 初始化 MCP 同步客户端列表
        // 原理：每个 ToolMcp 配置对应一个独立的外部服务连接（如本地进程 Stdio 或远程 SSE 服务）
        List<McpSyncClient> mcpSyncClients = new ArrayList<>();
        List<AiAgentConfigTableVO.Module.ChatModel.ToolMcp> toolMcpList = chatModelConfig.getToolMcpList();

        if (toolMcpList != null && !toolMcpList.isEmpty()) {
            log.debug("检测到 {} 个 MCP 工具配置，开始初始化客户端", toolMcpList.size());
            for (AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp : toolMcpList) {
                // createMcpSyncClient 内部会完成传输层建立、协议握手 (initialize) 及工具列表发现
                mcpSyncClients.add(createMcpSyncClient(toolMcp));
            }
        } else {
            log.debug("未配置 MCP 工具，将创建纯聊天模型实例");
        }

        // 4. 构建 Spring AI 的 ChatModel 实例
        // 核心组件：
        // - OpenAiChatModel: Spring AI 对 OpenAI 接口的标准实现，统一了 ChatClient 的调用入口
        // - OpenAiChatOptions: 运行时参数，包括模型名称、温度、TopP 等，以及关键的 ToolCallbacks
        // - SyncMcpToolCallbackProvider: 适配器模式，将 MCP 协议的工具描述转换为 Spring AI 可识别的 FunctionCallback
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi) // 注入通信底层
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(chatModelConfig.getModel()) // 指定具体模型，如 gpt-4o
                        .toolCallbacks(
                                // 仅当存在 MCP 客户端时才构建 Provider，避免空指针或无效开销
                                !mcpSyncClients.isEmpty() ?
                                        SyncMcpToolCallbackProvider.builder()
                                                .mcpClients(mcpSyncClients)
                                                .build()
                                                .getToolCallbacks()
                                        : null
                        )
                        .build())
                .build();

        // 5. 将构建好的 ChatModel 注入上下文
        // 作用：后续节点（如 Prompt 组装、执行节点）将从 Context 中获取此 Model 进行实际推理
        dynamicContext.setChatModel(chatModel);

        log.info("Ai Agent 装配操作 - ChatModelNode [模型构建完成, Model: {}]", chatModelConfig.getModel());

        // 6. 路由至下一节点
        // 模板方法模式：由父类决定下一步走向（通常是结束节点或后续的 Prompt 节点）
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return null;
    }

    private McpSyncClient createMcpSyncClient(AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp) throws Exception
    {
//        1.先拿到入参url
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sseConfig = toolMcp.getSse();
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdioConfig = toolMcp.getStdio();

        /**
         * 当程序发现 sseEndpoint 为空，而 originalBaseUri 又是一长串东西时，它会自动启动切割逻辑：
         * 代码先算出纯净的 baseUrl (http://appbuilder.baidu.com:8080)。
         * 然后拿原字符串 originalBaseUri 和算出来的 baseUrl 进行字符串相减（substring 截取）。
         * 切下来的后半截：/v2/ai_search/mcp/sse?api_key=12345，就被强行赋值给了 sseEndpoint。
         * 前半截：http://appbuilder.baidu.com:8080，被赋值给了准备传给底层 SDK 的 baseUri 变量。
         */

        // --- 分支 1: 基于 SSE (Server-Sent Events) 的远程连接 ---
        if(sseConfig != null)
        {
            log.debug("开始初始化 SSE 模式的 MCP 客户端, BaseUri: {}", sseConfig.getBaseUri());

                String originalBaseUri = sseConfig.getBaseUri();
                String baseUri = originalBaseUri;
                String sseEndpoint = sseConfig.getSseEndpoint();

                //将参数分离 切割url
                // 核心逻辑：分离 BaseUri 和 Endpoint
                // 许多 MCP 服务端配置可能直接给出一个完整的 SSE URL（例如 http://host/path/sse?token=xxx）。
                // Spring AI 的 HttpClientSseClientTransport 要求将 host/port 部分作为 baseUri，路径部分作为 sseEndpoint。

                if (StringUtils.isBlank(sseEndpoint))
                {
                    try {
                    //提取协议、主机、端口
                    //用Java原生的URl解析器
                    URL url = new URL(originalBaseUri);
                    String protocol = url.getProtocol();  // 协议
                    String host= url.getHost(); //域名
                    int port = url.getPort();  //-1 表示默认端口

                    //只保留了“协议 + 域名/IP + 端口”===>baseUrl
                    String baseUrl = port == -1? protocol +"//" + host: protocol +"//" + host + ":" + port;

                    // 提取原始 URL 中除去 baseUrl 后的剩余部分作为 endpoint
                    //拿原字符串 originalBaseUri 和算出来的 baseUrl 进行字符串相减（substring 截取）

                    //查找子串在母串中第一次出现的索引位置。如果找不到，就返回 -1。
                    int index = originalBaseUri.indexOf(baseUri);

                    //如果 index等于 -1则跳过
                    if(index != -1){
                        //指定的起始位置，一直截取到字符串的最后--把baseurl后面的剩余部分截取出来
                        String remainingPath = originalBaseUri.substring(index + baseUri.length());
                        //检查是否为空
                        if (!StringUtils.isBlank(remainingPath)){
                            sseEndpoint= remainingPath;
                        }

                    }
                    baseUri  = baseUrl;
                }
                catch (Exception e)
                {
                    log.warn("解析 SSE BaseUri 失败，将尝试使用原始配置: {}", e.getMessage());
                }

                 //构建McpSyncClient
                 // 兜底默认值：MCP 规范推荐的默认 SSE 端点
                sseEndpoint = StringUtils.isBlank(sseEndpoint) ? "/sse" : sseEndpoint;

                log.debug("SSE 连接参数 -> BaseUri: {}, Endpoint: {}", baseUri, sseEndpoint);


            }
            // 构建 SSE 传输层
            HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport
                    .builder(baseUri)
                    .sseEndpoint(sseEndpoint)
                    .build();

            // 构建同步客户端并设置请求超时
            McpSyncClient mcpSyncClient = McpClient
                    .sync(sseClientTransport)
                    .requestTimeout(Duration.ofMillis(sseConfig.getRequestTimeout()))
                    .build();

            // 执行协议初始化握手
            // 此步骤会发送 "initialize" 请求，等待服务端响应能力及工具列表
            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();
            log.info("MCP SSE 客户端初始化成功, Server Info: {}", initialize.serverInfo());

            return mcpSyncClient;

        }

        // --- 分支 2: 基于 Stdio (标准输入输出) 的本地进程连接 ---
        if (null != stdioConfig) {
            log.debug("开始初始化 Stdio 模式的 MCP 客户端, Command: {}", stdioConfig.getServerParameters().getCommand());

            AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters serverParameters = stdioConfig.getServerParameters();

            // 构建进程启动参数
            // Command: 可执行文件路径 (如 python, node, 或具体的二进制文件)
            // Args: 启动参数 (如 script.py, --port 8080)
            // Env: 环境变量 (如 API Keys, Path 等)
            ServerParameters stdioParams = ServerParameters.builder(serverParameters.getCommand())
                    .args(serverParameters.getArgs())
                    .env(serverParameters.getEnv())
                    .build();

            // 构建 Stdio 传输层
            // 注意：必须传入 JsonMapper，因为 Stdio 传输的是纯文本流，需要序列化/反序列化 JSON-RPC 消息
            StdioClientTransport stdioClientTransport = new StdioClientTransport(stdioParams, new JacksonMcpJsonMapper(new ObjectMapper()));

            // 构建同步客户端
            // 注意：Stdio 的超时时间单位通常较大，因为涉及进程启动开销，此处沿用配置中的秒级单位
            McpSyncClient mcpSyncClient = McpClient.sync(stdioClientTransport)
                    .requestTimeout(Duration.ofSeconds(stdioConfig.getRequestTimeout()))
                    .build();

            // 执行协议初始化握手
            McpSchema.InitializeResult initialize = mcpSyncClient.initialize();
            log.info("MCP Stdio 客户端初始化成功, Server Info: {}", initialize.serverInfo());

            return mcpSyncClient;
        }

        // 防御性编程：如果既没有 SSE 也没有 Stdio 配置，说明配置数据损坏或逻辑遗漏
        throw new RuntimeException("Invalid MCP Configuration: Both SSE and Stdio parameters are null. Please check the agent config.");

    }
}
