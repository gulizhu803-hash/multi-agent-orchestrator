package org.example.test.api.tool;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.time.Duration;

/**
 * Spring Ai Tool
 *

 */
@Slf4j
public class SpringAiToolTest {

    public static void main(String[] args) {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://apis.itedus.cn")
                .apiKey(System.getenv("AI_AGENT_OPENAI_API_KEY"))
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ChatModel chatModel;
        try {
            System.out.println("=== 正在初始化 MCP 客户端... ===");
            McpSyncClient mcpClient = sseMcpClient();
            System.out.println("=== MCP 客户端初始化成功 ===");
            chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model("gpt-4.1")
                            .toolCallbacks(SyncMcpToolCallbackProvider.builder()
                                    .mcpClients(mcpClient).build()
                                    .getToolCallbacks())
                            .build())
                    .build();
        } catch (Exception e) {
            System.err.println("⚠️ MCP 客户端初始化失败: " + e.getMessage());
            System.err.println("🔄 降级为不带工具的 ChatModel，继续执行...");
            chatModel = OpenAiChatModel.builder()
                    .openAiApi(openAiApi)
                    .defaultOptions(OpenAiChatOptions.builder()
                            .model("gpt-4.1")
                            .build())
                    .build();
        }

        System.out.println("🚀 正在调用 AI 模型...");
        String call = chatModel.call("你哪有哪些工具能力");

        System.out.println("\n✅ 测试结果: " + call);
    }

    /**
     * 百度搜索MCP服务(url)；https://sai.baidu.com/zh/detail/e014c6ffd555697deabf00d058baf388
     * 百度搜索MCP服务(key)；https://console.bce.baidu.com/iam/?_=1753597622044#/iam/apikey/list
     */
    public static McpSyncClient sseMcpClient() {

        // 自己申请 api_key
        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport.builder("https://appbuilder.baidu.com/v2/ai_search/mcp/")
                .sseEndpoint("sse?api_key=" + System.getenv("AI_AGENT_MCP_API_KEY"))
                .build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();
        
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
