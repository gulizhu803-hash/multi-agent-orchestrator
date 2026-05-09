package org.example.test.app;

import com.alibaba.fastjson.JSON;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import com.google.rpc.context.AttributeContext;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import org.junit.jupiter.api.Assertions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@SpringBootTest(properties = {
    "logging.level.root=INFO",
    "logging.level.org.example=DEBUG",
    "logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
})
@org.springframework.test.context.ActiveProfiles("test")
public class AiAgentAutoConfigTest {

    /**
     * 注入上下文
     */

    @Resource
    private ApplicationContext applicationContext;

    @Test
    public void test_agent() throws InterruptedException {
        log.info("========== 测试开始 ==========");

        // 先检查配置是否加载
        try {
            Object configProps = applicationContext.getBean("org.example.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties");
            log.info("✅ 配置 Bean 存在: {}", configProps.getClass().getName());
        } catch (Exception e) {
            log.error("❌ 配置 Bean 不存在!", e);
        }

        /**
         * 获取注册的 Agent--从yml配置文件中
         */
        log.info("开始测试 Agent 装配...");
        
        // 先打印所有已注册的 Bean 名称，调试用
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        log.info("已注册的 Bean 数量: {}", beanNames.length);
        
        // 列出所有 AiAgentRegisterVO 类型的 Bean
        String[] agentBeans = applicationContext.getBeanNamesForType(AiAgentRegisterVO.class);
        log.info("当前已注册的 AiAgentRegisterVO Bean: {}", java.util.Arrays.toString(agentBeans));
        
        // 检查是否有 agentId 为 100003 的 Bean
        if (!applicationContext.containsBean("100003")) {
            log.error("❌ Bean '100003' 不存在！请检查:");
            log.error("1. application-test.yml 是否正确加载");
            log.error("2. agent/only-one-agent.yml 配置文件是否存在且格式正确");
            log.error("3. AiAgentAutoConfig 是否成功执行装配");
            log.error("4. 配置文件是否包含 runner.agent-name 配置");
            throw new IllegalStateException("Agent '100003' 未注册，装配流程可能失败");
        }

        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100003", AiAgentRegisterVO.class);
        log.info("✅ 成功获取 Agent: {} (ID: {})", aiAgentRegisterVO.getAgentName(), aiAgentRegisterVO.getAgentId());

        String appName = aiAgentRegisterVO.getAppName();
        log.info("应用名称: {}", appName);

        /**
         * 构建运行时对象
         */
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        log.info("✅ InMemoryRunner 创建成功");

        // 创建会话
        Session session = runner.sessionService().createSession(appName, "test_user_456").blockingGet();
        log.info("✅ 会话创建成功, Session ID: {}", session.id());

        // 发送消息
        Content userMsg = Content.fromParts(Part.fromText("给我一份学习计划"));
        log.info("📤 发送消息: 给我一份学习计划");

        // 执行 Agent
        Flowable<Event> events = runner.runAsync("test_user_456", session.id(), userMsg);
        List<String> outputs = new ArrayList<>();

        try {
            // 设置超时时间为 60 秒
            events.timeout(60, TimeUnit.SECONDS)
                .blockingForEach(event -> {
                    String content = event.stringifyContent();
                    outputs.add(content);
                    log.info("📨 收到 Event: {}", content);
                    System.out.println("[STDOUT] Event: " + content);
                });
        } catch (Exception e) {
            log.error("❌ Agent 执行失败!", e);
            System.out.println("[STDOUT] ❌ 错误: " + e.getMessage());
        }

        System.out.println("[STDOUT] ========== 测试结果 ==========");
        System.out.println("[STDOUT] 收到 " + outputs.size() + " 个事件");
        if (!outputs.isEmpty()) {
            System.out.println("[STDOUT] " + JSON.toJSONString(outputs));
        }
        log.info("✅ 测试结果: 共收到 {} 个事件", outputs.size());
        System.out.println("[STDOUT] ========== 测试结束 ==========");
    }

    @Test
    public void test_handlerMessage_03(){
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100003", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService()
                .createSession(appName, "xiaofuge")
                .blockingGet();

        Content userMsg = Content.fromParts(Part.fromText("给我一份学习计划"));
        log.info("📤 发送消息: 给我一份学习计划");
        Flowable<Event> events = runner.runAsync("xiaofuge", session.id(), userMsg);

        List<String> outputs = new ArrayList<>();
        try {
            events.timeout(60, TimeUnit.SECONDS)
                    .blockingForEach(event -> {
                        if (event.finalResponse()) {
                            String content = event.stringifyContent();
                            if (content != null) {
                                outputs.add(content);
                                System.out.println("[AGENT_RESPONSE] " + content);
                            }
                        }
                    });
        } catch (Exception e) {
            // RxJava 的 timeout 操作符会在超时时抛出 RuntimeException（包含 TimeoutException）
            if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                log.error("❌ Agent 执行超时，已收到 {} 个事件", outputs.size());
                throw new AssertionError("Agent执行超时，60秒内未完成", e);
            }
            log.error("❌ Agent 执行失败", e);
            throw new AssertionError("Agent执行异常: " + e.getMessage(), e);
        }

        log.info("测试结果:{}", JSON.toJSONString(outputs));

        Assertions.assertFalse(outputs.isEmpty(), "Agent应返回至少一个非空事件");
        String fullOutput = String.join(" ", outputs);
        Assertions.assertTrue(fullOutput.contains("学习"),
                "返回内容应包含'学习'相关文本，实际输出:" + fullOutput);
    }
    @Test
    public void test_handlerMessage_04(){
        log.info("========== 开始测试 test_handlerMessage_04 ==========");
        
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100003", AiAgentRegisterVO.class);

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        Session session = runner.sessionService()
                .createSession(appName, "xiaofuge")
                .blockingGet();
        log.info("✅ 会话创建成功: {}", session.id());

        Content userMsg = Content.fromParts(Part.fromText("把xiaofuge转换为大写"));
        log.info("📤 发送消息: 把xiaofuge转换为大写");
        
        Flowable<Event> events = runner.runAsync("xiaofuge", session.id(), userMsg);

        List<String> outputs = new ArrayList<>();
        try {
            events.timeout(60, TimeUnit.SECONDS)
                .blockingForEach(event -> {
                    String content = event.stringifyContent();
                    outputs.add(content);
                    log.info("📨 收到 Event: {}", content);
                    System.out.println("[STDOUT] Event: " + content);
                });
        } catch (Exception e) {
            log.error("❌ Agent 执行失败!", e);
            System.out.println("[STDOUT] ❌ 错误: " + e.getMessage());
            throw new AssertionError("Agent执行异常: " + e.getMessage(), e);
        }

        System.out.println("[STDOUT] ========== 测试结果 ==========");
        System.out.println("[STDOUT] 收到 " + outputs.size() + " 个事件");
        if (!outputs.isEmpty()) {
            System.out.println("[STDOUT] " + JSON.toJSONString(outputs));
        }
        log.info("✅ 测试结果: 共收到 {} 个事件", outputs.size());
        log.info("测试结果:{}", JSON.toJSONString(outputs));
        
        // 添加断言验证结果
        Assertions.assertFalse(outputs.isEmpty(), "Agent应返回至少一个非空事件");
        String fullOutput = String.join(" ", outputs);
        Assertions.assertTrue(fullOutput.toUpperCase().contains("XIAOFUGE"),
                "返回内容应包含'XIAOFUGE'（大写形式），实际输出:" + fullOutput);
        
        System.out.println("[STDOUT] ========== 测试结束 ==========");
    }


}
