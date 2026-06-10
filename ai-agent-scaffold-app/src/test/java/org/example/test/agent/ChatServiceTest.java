package org.example.test.agent;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.IChatService;
import org.example.domain.agent.model.entity.ChatCommandEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.MimeTypeUtils;
import org.springframework.test.context.ActiveProfiles;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.List;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class ChatServiceTest {

    @Resource
    private IChatService chatService;

    @Value("classpath:file/dog.png")
    private org.springframework.core.io.Resource imageResource;


    @Test
    public void test_handleMessage_01() {
        List<String> message = chatService.handleMessage("100003", "test-user", "你具备哪些能力");
        log.info("测试结果:{}", JSON.toJSONString(message));
    }

    @Test
    public void test_handleMessage_04_withImage() throws IOException {
        String agentId = "100003";
        String userId = "test-user";

        String sessionId = chatService.createSession(agentId, userId);

        // 读取图片文件为字节数组
        byte[] imageBytes = imageResource.getInputStream().readAllBytes();

        ChatCommandEntity chatCommandEntity = ChatCommandEntity.builder()
                .agentId(agentId)
                .userId(userId)
                .sessionId(sessionId)
                .texts(List.of(new ChatCommandEntity.Content.Text("请识别这个图片。告诉我它是什么动物，并用一句话描述。")))
                .files(List.of())
                .inlineDatas(List.of(new ChatCommandEntity.Content.InlineData(imageBytes, MimeTypeUtils.IMAGE_PNG_VALUE)))
                .build();

        List<String> message = chatService.handleMessage(chatCommandEntity);
        log.info("测试结果:{}", JSON.toJSONString(message));
    }

}
