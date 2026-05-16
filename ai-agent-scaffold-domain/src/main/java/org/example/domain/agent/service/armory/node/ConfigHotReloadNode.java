package org.example.domain.agent.service.armory.node;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.IArmoryService;
import org.example.domain.agent.model.entity.ConfigSaveCommandEntity;
import org.example.domain.agent.service.armory.AbstractConfigSupport;
import org.example.domain.agent.service.armory.ConfigFactory;
import org.example.domain.agent.service.armory.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;

@Slf4j
@Service
public class ConfigHotReloadNode extends AbstractConfigSupport {

    @Resource
    private IArmoryService armoryService;

    @Override
    protected String doApply(ConfigSaveCommandEntity requestParameter, ConfigFactory.DynamicContext dynamicContext) throws Exception {
        log.info("智能体配置 - 热装配");

        armoryService.acceptArmoryAgents(Collections.singletonList(dynamicContext.getTableVO()));

        log.info("智能体 {} 热装配完成", requestParameter.getAgentConfig().getAppName());
        return dynamicContext.getFilename();
    }

    @Override
    public StrategyHandler<ConfigSaveCommandEntity, ConfigFactory.DynamicContext, String> get(ConfigSaveCommandEntity requestParameter, ConfigFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
}
