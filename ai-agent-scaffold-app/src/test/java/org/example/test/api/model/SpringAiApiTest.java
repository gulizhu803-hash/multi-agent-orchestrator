package org.example.test.api.model;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI Test
 * 文档：<a href="https://docs.spring.io/spring-ai/reference/1.0/api/advisors.html">spring ai</a>
 */
@Slf4j
public class SpringAiApiTest {

    @Test
    public void testChatModel() throws IOException {
        // 加载 env-config.properties，与主项目配置一致
        Properties env = new Properties();
        try (FileReader reader = new FileReader("env-config.properties")) {
            env.load(reader);
        } catch (IOException e) {
            log.warn("未找到 env-config.properties，回退到 System.getenv()");
        }

        String baseUrl = env.getProperty("AI_BASE_URL", System.getenv("AI_BASE_URL"));
        String apiKey = env.getProperty("AI_AGENT_OPENAI_API_KEY", System.getenv("AI_AGENT_OPENAI_API_KEY"));
        String completionsPath = env.getProperty("AI_COMPLETIONS_PATH", System.getenv("AI_COMPLETIONS_PATH"));
        String model = env.getProperty("AI_MODEL", System.getenv("AI_MODEL"));

        assertNotNull(apiKey, "AI_AGENT_OPENAI_API_KEY 未配置，请在 env-config.properties 中设置");

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .completionsPath(completionsPath != null ? completionsPath : "v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model != null ? model : "qwen-plus")
                        .build())
                .build();

        String call = chatModel.call("hi 你好哇!");

        log.info("测试结果:{}", call);

        assertNotNull(call, "AI 响应不应为空");
        assertTrue(call.length() > 0, "AI 响应应包含内容");
    }

}
