package org.example.domain.agent.service.armory.factory;

import org.example.domain.agent.service.armory.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.SequentialAgent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.service.armory.node.RootNode;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class DefaultArmoryFactory {
    @Resource
    private ApplicationContext applicationContext;

    @Resource
    private RootNode rootNode;
    
    private static StrategyHandler<ArmoryCommandEntity, DynamicContext, AiAgentRegisterVO> strategyHandler;
    
    @PostConstruct
    public void init() {
        strategyHandler = rootNode;
    }
    
    public static StrategyHandler<ArmoryCommandEntity, DynamicContext, AiAgentRegisterVO> armoryStrategyHandler() {
        return strategyHandler;
    }
    public AiAgentRegisterVO getAiAgentRegisterVO(String agentId) {
        return applicationContext.getBean(agentId, AiAgentRegisterVO.class);
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DynamicContext {

        /**
         * LLM API
         */
        private OpenAiApi openAiApi;

        /**
         * LLM ChatModel
         */
        private ChatModel chatModel;

        /**
         * 当做最后一个智能体节点
         */
        private SequentialAgent sequentialAgent;

        /**
         * 智能体配置组
         */
        private Map<String, BaseAgent> agentGroup = new HashMap<>();

//        private List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = new ArrayList<>();

        private Map<String, Object> dataObjects = new HashMap<>();

        private AiAgentConfigTableVO.Module.AgentWorkflow currentAgentWorkflow;
        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }

        //计算步数
        private AtomicInteger currentStepIndex = new AtomicInteger(0);
        public List<BaseAgent> queryAgentList(List<String> agentNames) {
            if (agentNames == null || agentNames.isEmpty() || agentGroup == null) {
                return Collections.emptyList();
            }

            List<BaseAgent> agents = new ArrayList<>();
            for (String name : agentNames) {
                BaseAgent agent = agentGroup.get(name);
                if (agent!=null){
                    agents.add(agent);
                }
            }

            return agents;
        }
        public void addCurrentStepIndex() {
            currentStepIndex.incrementAndGet();
        }

        public int getCurrentStepIndex() {
            return currentStepIndex.get();
        }
    }

}
