package org.example.domain.agent.service.armory.node.workflow;

import org.example.domain.agent.service.armory.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LoopAgent;
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
public class LoopAgentNode extends AbstractArmorySupport {
    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - LoopAgentNode");
        AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow = dynamicContext.getCurrentAgentWorkflow();

      //获取subAgents
        List<String> subAgentsName = currentAgentWorkflow.getSubAgents();
        List<BaseAgent> subAgents = dynamicContext.queryAgentList(subAgentsName);

      // 构建循环agent
      LoopAgent loopAgent =
            LoopAgent.builder()
                    .name(currentAgentWorkflow.getName())
                    .description(currentAgentWorkflow.getDescription())
                    .subAgents(subAgents)
                    .maxIterations(currentAgentWorkflow.getMaxIterations())
                    .build();

        dynamicContext.getAgentGroup().put(currentAgentWorkflow.getName(), loopAgent);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
//        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = dynamicContext.getAgentWorkflows();
//
//
//        if (agentWorkflows == null ||agentWorkflows.isEmpty())
//        {
//            return  defaultStrategyHandler;
//        }
//
//        AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow = agentWorkflows.get(0);
//
//
//
//        String type = agentWorkflow.getType();
//        AgentTypeEnum typeEnum = AgentTypeEnum.formType(type);
//
//        if (null == typeEnum) {
//            throw new RuntimeException("agentWorkflow type is error!");
//        }
//        String node = typeEnum.getNode();
//
//        return switch (node)
//        {
//            case "sequentialAgentNode" -> getBean("sequentialAgentNode");
//            case "parallelAgentNode" -> getBean("parallelAgentNode");
//            default -> defaultStrategyHandler;
//        };

        return getBean("agentWorkflowNode");

    }
}
