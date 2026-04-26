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

import java.util.ArrayList;
import java.util.List;

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
    public void test_agent()throws InterruptedException{
        log.info("========== 测试开始 ==========");

        // 先检查配置是否加载
        try {
            Object configProps = applicationContext.getBean("org.example.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties");
            log.info("配置 Bean 存在: {}", configProps.getClass().getName());
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
        
        // 检查是否有 agentId 为 100001 的 Bean
        if (!applicationContext.containsBean("100001")) {
            log.error("Bean '100001' 不存在！请检查:");
            log.error("1. application-dev.yml 是否正确加载");
            log.error("2. agent/test_agent.yml 配置文件是否存在");
            log.error("3. AiAgentAutoConfig 是否成功执行装配");
            log.error("4. 数据库连接是否正常");
            
            // 列出所有 AiAgentRegisterVO 类型的 Bean
            String[] agentBeans = applicationContext.getBeanNamesForType(AiAgentRegisterVO.class);
            log.info("当前已注册的 AiAgentRegisterVO Bean: {}", java.util.Arrays.toString(agentBeans));
            
            throw new IllegalStateException("Agent '100001' 未注册，装配流程可能失败");
        }
        
        AiAgentRegisterVO aiAgentRegisterVO = applicationContext.getBean("100001", AiAgentRegisterVO.class);
        log.info("成功获取 Agent: {}", aiAgentRegisterVO.getAgentName());

        String appName = aiAgentRegisterVO.getAppName();

        /**
         * 构建运行时对象
         */

        InMemoryRunner  runner = aiAgentRegisterVO.getRunner();

         Session session = runner.sessionService().createSession(appName, "test_user_456").blockingGet();
         Content userMsg = Content.fromParts(Part.fromText("编写冒泡排序"));

         Flowable<Event> events =  runner.runAsync("test_user_456", session.id(), userMsg);
        List<String> outputs = new ArrayList<>();
        events.blockingForEach(event -> {
            String content = event.stringifyContent();
            outputs.add(content);
            log.info("收到 Event: {}", content);
            System.out.println("[STDOUT] Event: " + content);
        });

        System.out.println("[STDOUT] ========== 测试结果 ==========");
        System.out.println("[STDOUT] " + JSON.toJSONString(outputs));
        log.info("测试结果:{}", JSON.toJSONString(outputs));
        System.out.println("[STDOUT] ========== 测试结束 ==========");
    }


}
