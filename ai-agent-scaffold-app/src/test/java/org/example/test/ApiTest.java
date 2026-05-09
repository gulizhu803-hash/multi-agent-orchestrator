package org.example.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
@SpringBootTest
public class ApiTest {

    @Test
    public void test() {
        log.info("测试完成");
        assertTrue(true, "测试应该通过");
    }

}
