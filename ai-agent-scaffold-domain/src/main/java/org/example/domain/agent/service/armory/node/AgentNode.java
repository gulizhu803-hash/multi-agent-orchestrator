package org.example.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.LlmAgent;
import com.google.adk.models.springai.SpringAI;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.service.armory.AbstractArmorySupport;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class AgentNode extends AbstractArmorySupport{

    @Resource
    private AgentWorkflowNode agentWorkflowNode;
    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - AgentNode");

        // 1. 获取底层大模型交互接口
        // ChatModel 是 Spring AI 提供的统一接口，屏蔽了不同厂商（OpenAI, Azure, Qwen 等）的差异
        ChatModel chatModel = dynamicContext.getChatModel();

        // 2. 提取 Agent 配置列表
        // 从请求参数中获取完整的 Agent 配置表，并定位到模块下的 Agents 列表
        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        List<AiAgentConfigTableVO.Module.Agent> agents = aiAgentConfigTableVO.getModule().getAgents();

        // 3. 遍历配置，实例化并注册每个 Agent
        for (AiAgentConfigTableVO.Module.Agent agentConfig : agents) {
            // 构建 LlmAgent 实例
            // LlmAgent 是 Google ADK 框架中代表一个智能体的核心类，它封装了模型调用、提示词管理和输出解析
            LlmAgent llmAgent = LlmAgent.builder()
                    .name(agentConfig.getName())              // 设置代理唯一标识，用于路由匹配
                    .description(agentConfig.getDescription()) // 设置代理功能描述，辅助 LLM 进行路由决策
                    .model(new SpringAI(chatModel))           // 适配 Spring AI 的 ChatModel 为 ADK 的 Model 接口
                    .instruction(agentConfig.getInstruction()) // 设置系统指令，定义代理的角色和行为边界
                    .outputKey(agentConfig.getOutputKey())    // 设置输出键，指定代理响应存储在上下文中的位置
                    .build();

            // 将构建好的 Agent 注册到动态上下文的 Agent 组中
            // key: agentName, value: LlmAgent instance
            // 这一步使得该 Agent 在当前会话的工作流中可见并可被调用
            dynamicContext.getAgentGroup().put(agentConfig.getName(), llmAgent);
        }
        // 4. 路由到下一个节点
        // 装配完成后，根据策略将控制权移交给下一个处理节点，继续工作流执行
        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return agentWorkflowNode;
    }
}
