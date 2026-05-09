package org.example.test.api.model;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Spring AI Test
 * 文档：<a href="https://docs.spring.io/spring-ai/reference/1.0/api/advisors.html">spring ai</a>
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/12/14 09:15
 */
@Slf4j
public class SpringAiApiTest {

    @Test
    public void testChatModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://apis.itedus.cn/")
                .apiKey("sk-wtBOjyNviG9NtbYn7f2fF8A2203048Aa86Be6f0f0b824dB9")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1")
                        .build())
                .build();

        String call = chatModel.call("hi 你好哇!");

        log.info("测试结果:{}", call);
        
        // 添加断言来验证测试结果
        assertNotNull(call, "AI 响应不应为空");
        assertTrue(call.length() > 0, "AI 响应应包含内容");
    }

}
