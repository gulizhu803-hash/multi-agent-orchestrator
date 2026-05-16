package org.example.domain.agent.service.armory.node;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.model.entity.ConfigSaveCommandEntity;
import org.example.domain.agent.model.valobj.AgentConfigVO;
import org.example.domain.agent.service.armory.AbstractConfigSupport;
import org.example.domain.agent.service.armory.ConfigFactory;
import org.example.domain.agent.service.armory.tree.StrategyHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;

@Slf4j
@Service
public class ConfigValidateNode extends AbstractConfigSupport {

    @Resource
    private ConfigAssemblyNode configAssemblyNode;

    @Override
    protected String doApply(ConfigSaveCommandEntity requestParameter, ConfigFactory.DynamicContext dynamicContext) throws Exception {
        log.info("智能体配置 - 参数校验");

        AgentConfigVO config = requestParameter.getAgentConfig();
        Assert.hasText(config.getAppName(), "appName 不能为空");
        Assert.hasText(config.getAgentId(), "agentId 不能为空");
        Assert.hasText(config.getBaseUrl(), "baseUrl 不能为空");
        Assert.hasText(config.getApiKey(), "apiKey 不能为空");
        Assert.hasText(config.getModel(), "model 不能为空");
        Assert.notEmpty(config.getAgents(), "至少需要一个 agent 定义");

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ConfigSaveCommandEntity, ConfigFactory.DynamicContext, String> get(ConfigSaveCommandEntity requestParameter, ConfigFactory.DynamicContext dynamicContext) throws Exception {
        return configAssemblyNode;
    }
}
