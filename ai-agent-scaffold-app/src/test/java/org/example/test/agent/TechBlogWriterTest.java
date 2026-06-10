package org.example.test.agent;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.IChatService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import jakarta.annotation.Resource;
import java.util.List;

@Slf4j
@SpringBootTest(properties = {
        "spring.profiles.active=test"
})
public class TechBlogWriterTest {

    @Resource
    private IChatService chatService;

    @Test
    public void test_write_blog_basic() {
        List<String> result = chatService.handleMessage("300001", "test_user",
                "写一篇关于 Java Optional 类的简短技术博客，介绍核心概念、常用方法和最佳实践，包含代码示例");

        log.info("测试结果:{}", JSON.toJSONString(result));
    }

    @Test
    public void test_write_blog_with_code() {
        List<String> result = chatService.handleMessage("300001", "test_user",
                """
                帮我分析下面这段代码，写一篇技术博客解释它的作用和设计思路：

                public class Singleton {
                    private static volatile Singleton instance;
                    private Singleton() {}
                    public static Singleton getInstance() {
                        if (instance == null) {
                            synchronized (Singleton.class) {
                                if (instance == null) {
                                    instance = new Singleton();
                                }
                            }
                        }
                        return instance;
                    }
                }
                """);

        log.info("测试结果:{}", JSON.toJSONString(result));
    }
}
