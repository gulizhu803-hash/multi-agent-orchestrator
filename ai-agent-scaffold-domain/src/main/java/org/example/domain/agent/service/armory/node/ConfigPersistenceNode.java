package org.example.domain.agent.service.armory.node;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.model.entity.ConfigSaveCommandEntity;
import org.example.domain.agent.service.armory.AbstractConfigSupport;
import org.example.domain.agent.service.armory.ConfigFactory;
import org.example.domain.agent.service.armory.repository.IConfigRepository;
import org.example.domain.agent.service.armory.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class ConfigPersistenceNode extends AbstractConfigSupport {

    @Resource
    private IConfigRepository configRepository;

    @Resource
    private ConfigHotReloadNode configHotReloadNode;

    @Override
    protected String doApply(ConfigSaveCommandEntity requestParameter, ConfigFactory.DynamicContext dynamicContext) throws Exception {
        log.info("智能体配置 - 持久化");

        String filename = configRepository.save(
                requestParameter.getAgentConfig().getAppName(),
                dynamicContext.getYamlContent()
        );
        dynamicContext.setFilename(filename);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ConfigSaveCommandEntity, ConfigFactory.DynamicContext, String> get(ConfigSaveCommandEntity requestParameter, ConfigFactory.DynamicContext dynamicContext) throws Exception {
        return configHotReloadNode;
    }
}
