package org.example.test.api.tool;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * LangChain4j
 * <p>
 * 文档：<a href="https://docs.langchain4j.info/">langchain4j</a>
 * 案例：<a href="https://github.com/langchain4j/langchain4j-examples">langchain4j-examples</a>
 *
 */
@Slf4j
public class LangChain4jToolTest {

    interface Assistant {
        String chat(String message);
    }

    public static void main(String[] args) {
        System.out.println("=== 正在初始化 LangChain4j 模型... ===");
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("https://apis.itedus.cn/v1")
                .apiKey(System.getenv("AI_AGENT_OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        System.out.println("=== 正在初始化 MCP 客户端... ===");
        Assistant assistant;
        try {
            assistant = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    .tools(sseMcpClient())
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .build();
            System.out.println("=== LangChain4j 装配成功 ===");
        } catch (Exception e) {
            System.err.println("⚠️ MCP 客户端初始化失败: " + e.getMessage());
            System.err.println("🔄 降级为不带工具的 Assistant，继续执行...");
            assistant = AiServices.builder(Assistant.class)
                    .chatModel(model)
                    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                    .build();
        }

        System.out.println("🚀 正在调用 AI 模型...");
        String answer = assistant.chat("你哪有哪些工具能力");
        System.out.println("\n✅ 测试结果: " + answer);
    }

    /**
     * 百度搜索MCP服务(url)；https://sai.baidu.com/zh/detail/e014c6ffd555697deabf00d058baf388
     * 百度搜索MCP服务(key)；https://console.bce.baidu.com/iam/?_=1753597622044#/iam/apikey/list
     */
    public static McpSyncClient sseMcpClient() {

        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport.builder("https://appbuilder.baidu.com/v2/ai_search/mcp/")
                .sseEndpoint("sse?api_key=" + System.getenv("AI_AGENT_MCP_API_KEY"))
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofSeconds(30)).build();
        try {
            System.out.println("[MCP] 开始初始化...");
            var init_sse = mcpSyncClient.initialize();
            System.out.println("[MCP] 初始化成功: " + init_sse);
        } catch (Exception e) {
            System.err.println("[MCP] 初始化失败: " + e.getMessage());
            throw new RuntimeException("MCP 客户端初始化超时或失败，请检查 API Key 是否有效以及网络连接", e);
        }

        return mcpSyncClient;
    }

}
