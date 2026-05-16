package org.example.domain.agent.service.config;

import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.IConfigService;
import org.example.domain.agent.model.entity.ConfigSaveCommandEntity;
import org.example.domain.agent.model.valobj.AgentConfigVO;
import org.example.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import org.example.domain.agent.service.armory.ConfigFactory;
import org.example.domain.agent.service.armory.assembly.IConfigAssembler;
import org.example.domain.agent.service.armory.repository.IConfigRepository;
import org.example.domain.agent.service.armory.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConfigService implements IConfigService {

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    @Resource
    private IConfigAssembler configAssembler;

    @Resource
    private IConfigRepository configRepository;

    @Override
    public String saveAgentConfig(ConfigSaveCommandEntity command) throws Exception {
        StrategyHandler<ConfigSaveCommandEntity, ConfigFactory.DynamicContext, String> handler =
                ConfigFactory.configStrategyHandler();
        return handler.apply(command, new ConfigFactory.DynamicContext());
    }

    @Override
    public List<AgentConfigVO> listAgentConfigs() {
        if (aiAgentAutoConfigProperties.getTables() == null) {
            return Collections.emptyList();
        }
        return aiAgentAutoConfigProperties.getTables().values().stream()
                .map(configAssembler::fromTableVO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAgentConfig(String appName) throws Exception {
        configRepository.delete(appName);
    }
}
