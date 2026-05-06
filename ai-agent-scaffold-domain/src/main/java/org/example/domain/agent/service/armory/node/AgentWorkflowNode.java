package org.example.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.model.valobj.enums.AgentTypeEnum;
import org.example.domain.agent.service.armory.AbstractArmorySupport;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.example.domain.agent.service.armory.node.workflow.LoopAgentNode;
import org.example.domain.agent.service.armory.node.workflow.ParallelAgentNode;
import org.example.domain.agent.service.armory.node.workflow.SequentialAgentNode;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * ：负责将配置中的 Agent 定义实例化为 Google ADK 的 LlmAgent 对象，并注册到动态上下文中。
 *
 */
@Slf4j
@Service
public class AgentWorkflowNode extends AbstractArmorySupport {


    @Resource
    private LoopAgentNode loopAgentNode;
    @Resource
    private ParallelAgentNode parallelAgentNode;
    @Resource
    private SequentialAgentNode sequentialAgentNode ;
    @Resource
    private RunnerNode runnerNode;


    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {

        log.info("Ai Agent 装配操作 - AgentWorkflowNode [开始执行工作流节点装配]");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = aiAgentConfigTableVO.getModule().getAgentWorkflows();

        /**
         * 检查是否为空
         */
        if (agentWorkflows == null || agentWorkflows.isEmpty())
        {
            log.error("Ai Agent 装配失败 - AgentWorkflowNode [agentWorkflows 配置为空]");

            return  router(requestParameter,dynamicContext);
        }

        dynamicContext.setAgentWorkflows(agentWorkflows);

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {

        List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = dynamicContext.getAgentWorkflows();
        if (agentWorkflows == null || agentWorkflows.isEmpty()) {
            return runnerNode;
        }
        AiAgentConfigTableVO.Module.AgentWorkflow agentWorkflow = agentWorkflows.get(0);  //获取第一个

        //判断类型
        String type = agentWorkflow.getType();
        AgentTypeEnum typeEnum = AgentTypeEnum.formType(type);
        if(typeEnum == null)
        {
            log.error("Ai Agent 装配失败 - AgentWorkflowNode [不支持的工作流类型: {}]", type);
            return  runnerNode;
        }
        String node = typeEnum.getNode();
        return switch (node){
            case "loopAgentNode" -> loopAgentNode;
            case "parallelAgentNode" -> parallelAgentNode;
            case "sequentialAgentNode" -> sequentialAgentNode;
            default -> defaultStrategyHandler;
        };
    }
}
