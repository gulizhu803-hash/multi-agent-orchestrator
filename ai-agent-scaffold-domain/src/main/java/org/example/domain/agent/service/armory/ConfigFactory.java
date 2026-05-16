package org.example.domain.agent.service.armory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.domain.agent.model.entity.ConfigSaveCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.service.armory.node.ConfigValidateNode;
import org.example.domain.agent.service.armory.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Service
public class ConfigFactory {

    @Resource
    private ConfigValidateNode configValidateNode;

    private static StrategyHandler<ConfigSaveCommandEntity, DynamicContext, String> strategyHandler;

    @PostConstruct
    public void init() {
        strategyHandler = configValidateNode;
    }

    public static StrategyHandler<ConfigSaveCommandEntity, DynamicContext, String> configStrategyHandler() {
        return strategyHandler;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        private String yamlContent;
        private AiAgentConfigTableVO tableVO;
        private String filename;
    }
}
