package org.example.domain.agent.service.armory.node;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.model.entity.ConfigSaveCommandEntity;
import org.example.domain.agent.model.valobj.AgentConfigVO;
import org.example.domain.agent.service.armory.AbstractConfigSupport;
import org.example.domain.agent.service.armory.ConfigFactory;
import org.example.domain.agent.service.armory.assembly.IConfigAssembler;
import org.example.domain.agent.service.armory.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Slf4j
@Service
public class ConfigAssemblyNode extends AbstractConfigSupport {

    @Resource
    private IConfigAssembler configAssembler;

    @Resource
    private ConfigPersistenceNode configPersistenceNode;

    @Override
    protected String doApply(ConfigSaveCommandEntity requestParameter, ConfigFactory.DynamicContext dynamicContext) throws Exception {
        log.info("智能体配置 - 装配转换");

        AgentConfigVO config = requestParameter.getAgentConfig();
        dynamicContext.setYamlContent(configAssembler.toYaml(config));
        dynamicContext.setTableVO(configAssembler.toTableVO(config));

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ConfigSaveCommandEntity, ConfigFactory.DynamicContext, String> get(ConfigSaveCommandEntity requestParameter, ConfigFactory.DynamicContext dynamicContext) throws Exception {
        return configPersistenceNode;
    }
}
