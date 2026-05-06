package org.example.domain.agent.service.armory.node.workflow;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.ParallelAgent;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.model.valobj.enums.AgentTypeEnum;
import org.example.domain.agent.service.armory.AbstractArmorySupport;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ParallelAgentNode extends AbstractArmorySupport {
    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = dynamicContext.getAgentWorkflows();
        AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow = agentWorkflows.remove(0);

        List<String> subAgentsname = agentWorkflow.getSubAgents();
        List<BaseAgent> subAgents = dynamicContext.queryAgentList(subAgentsname);

        ParallelAgent parallelResearchAgent =
                ParallelAgent.builder()
                        .name(agentWorkflow.getName())
                        .subAgents(subAgents)
                        .description(agentWorkflow.getDescription())
                        .build();

        dynamicContext.getAgentGroup().put(agentWorkflow.getName(), parallelResearchAgent);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = dynamicContext.getAgentWorkflows();

        if (agentWorkflows == null || agentWorkflows.isEmpty())
        {
            return defaultStrategyHandler;
        }
        AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow = agentWorkflows.get(0);
        String type = agentWorkflow.getType();
        AgentTypeEnum typeEnum = AgentTypeEnum.formType(type);
        if (null == typeEnum) {
            throw new RuntimeException("agentWorkflow type is error!");
        }


        String node = typeEnum.getNode();

        return  switch (node)
        {
            case "sequentialAgentNode" -> getBean("sequentialAgentNode");
            case "loopAgentNode" -> getBean("loopAgentNode");
            default -> defaultStrategyHandler;
        };

    }
}
