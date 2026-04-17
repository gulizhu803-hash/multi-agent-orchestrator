package org.example.domain.agent.service.armory.factory;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import com.google.adk.agents.BaseAgent;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultArmoryFactory {

    @Resource
    private static RootNode rootNode;
    public static StrategyHandler<ArmoryCommandEntity, DynamicContext, AiAgentRegisterVO> armoryStrategyHandler( ){
        return rootNode;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class DynamicContext {

        // OpenAI API 实例
        private OpenAiApi openAiApi;
        // 聊天模型实例
        private ChatModel chatModel;
        // 通用数据存储对象
        private Map<String, Object> dataObjects = new HashMap<>();
        // Agent 分组映射
        private Map<String, BaseAgent> agentGroup = new HashMap<>();

        // Agent 工作流列表
        private List<AiAgentConfigTableVO.Module.AgentWorkflow> agentWorkflows = new ArrayList<>();

        public <T> void setValue(String key, T value) {
            dataObjects.put(key, value);
        }

        public <T> T getValue(String key) {
            return (T) dataObjects.get(key);
        }

    }
}
