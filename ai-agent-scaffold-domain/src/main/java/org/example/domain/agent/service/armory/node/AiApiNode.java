package org.example.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.service.armory.AbstractArmorySupport;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.springframework.ai.openai.api.OpenAiApi;

public class AiApiNode  extends AbstractArmorySupport{

    @Resource
    private ChatModelNode chatModelNode;

    @Override
    protected AiAgentRegisterVO doApply(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {

        log.info("Ai Agent 装配操作 - AiApiNode");
//        1.拿到入参
        AiAgentConfigTableVO  aiAgentConfigTableVO = requestParameter.getAiAgentConfigTableVO();
        AiAgentConfigTableVO.Module.AiApi aiApiConfig  = aiAgentConfigTableVO.getModule().getAiApi();

//        2.构建OpenAiApi
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(aiApiConfig.getBaseUrl())
                .apiKey(aiApiConfig.getApiKey())
                .completionsPath(StringUtils.isNotBlank(aiApiConfig.getCompletionsPath()) ? aiApiConfig.getCompletionsPath() : "v1/chat/completions")
                .embeddingsPath(StringUtils.isNotBlank(aiApiConfig.getEmbeddingsPath()) ? aiApiConfig.getEmbeddingsPath() : "v1/embeddings")
                .build();

        dynamicContext.setOpenAiApi(openAiApi);
        return router(requestParameter, dynamicContext) ;
    }

    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> get(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws Exception {
        // 如果不需要下一个节点了，可以配置 defaultStrategyHandler
        return chatModelNode;
    }
}
