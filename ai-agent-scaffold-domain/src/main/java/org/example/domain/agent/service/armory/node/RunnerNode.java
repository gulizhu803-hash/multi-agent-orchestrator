package org.example.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.SequentialAgent;
import com.google.adk.runner.InMemoryRunner;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.service.armory.AbstractArmorySupport;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RunnerNode extends AbstractArmorySupport {
    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 装配操作 - RunnerNode [开始构建 InMemoryRunner]");

        AiAgentConfigTableVO aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        // 获取应用名称，用于标识 Runner 所属的应用上下文
        String appName = aiAgentConfigTableVO.getAppName();

        /**
         * 获取agent的具体配置
         */
        AiAgentConfigTableVO.Agent agent = aiAgentConfigTableVO.getAgent();
        String agentId =agent.getAgentId();
        String agentName = agent.getAgentName();
        String agentDesc = agent.getAgentDesc();

        /**
         * 获取workflow的容器 默认是sequence
         */
        SequentialAgent sequentialAgent = dynamicContext.getSequentialAgent();

        // 实例化 InMemoryRunner 让sequentialAgent跑起来
        InMemoryRunner runner = new InMemoryRunner(sequentialAgent, appName);

        // 3. 封装注册值对象（VO）
        // 将配置信息与运行时对象（Runner）打包，形成完整的 Agent 注册信息
        AiAgentRegisterVO aiAgentRegisterVO = AiAgentRegisterVO.builder()
                .appName(appName)
                .agentId(agentId)
                .agentName(agentName)
                .agentDesc(agentDesc)
                .runner(runner) // 核心：将可执行的 Runner 嵌入 VO
                .build();

        // 4. 动态注册到 Spring 容器
        // 将 aiAgentRegisterVO 以 agentId 为 Bean 名称注册到 Spring 上下文
        // 这使得其他服务可以通过 agentId 直接查找并复用该 Agent 实例，实现全局唯一性和依赖注入
        registerBean(agentId, AiAgentRegisterVO.class, aiAgentRegisterVO);

        log.info("Ai Agent 装配操作 - RunnerNode [完成] agentId: {}, appName: {}", agentId, appName);

        return aiAgentRegisterVO;

    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
}
