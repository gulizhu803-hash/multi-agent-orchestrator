package org.example.config;

import com.alibaba.fastjson.JSON;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.IArmoryService;
import org.example.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Slf4j
@Configuration
@EnableConfigurationProperties(AiAgentAutoConfigProperties.class)
public class AiAgentAutoConfig {

    public AiAgentAutoConfig() {
        log.info("🔧 AiAgentAutoConfig Bean 正在创建...");
    }

    @Resource
    private IArmoryService armoryService;

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @PostConstruct
    public void init() {
        try {
            log.info("========== Ai Agent 智能体装配开始 ==========");
            log.info("配置属性: {}", JSON.toJSONString(aiAgentAutoConfigProperties));
            
            if (aiAgentAutoConfigProperties.getTables() == null || aiAgentAutoConfigProperties.getTables().isEmpty()) {
                log.error("❌ 未找到任何 Agent 配置!请检查:");
                log.error("1. application-*.yml 中是否正确导入了 agent 配置文件");
                log.error("2. agent 配置文件路径是否正确(注意连字符和下划线的区别)");
                log.error("3. 配置文件格式是否符合 ai.agent.config.tables 结构");
                return;
            }
            
            log.info("Ai Agent 智能体装配 {}", JSON.toJSONString(aiAgentAutoConfigProperties.getTables().values()));
            // 装配操作
            // 从配置属性中获取所有需要装配的Agent表格数据，并转换为List
            // 调用armoryService的acceptArmoryAgents方法，执行Agent的装配逻辑
            armoryService.acceptArmoryAgents(new ArrayList<>(aiAgentAutoConfigProperties.getTables().values()));
            log.info("========== Ai Agent 智能体装配完成 ==========");

        } catch (Exception e) {
            log.error("❌ Ai Agent 装配失败!", e);
            throw new RuntimeException(e);
        }
    }

}
