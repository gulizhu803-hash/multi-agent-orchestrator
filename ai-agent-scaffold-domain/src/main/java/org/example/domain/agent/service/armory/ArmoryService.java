package org.example.domain.agent.service.armory;

import org.example.domain.agent.service.armory.tree.StrategyHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.agent.IArmoryService;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ArmoryService implements IArmoryService {
    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    public void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception
    {



        //    2. DTO 转 Command (Intent Translation)
        //    3.为每一次循环（每一个 Agent 的装配）new 了一个全新的、空白的动态上下文数据总线
        for (AiAgentConfigTableVO table : tables)
        {
            //    1.从工厂中获取责任链的入口点---调用armoryStrategyHandler方法--拿到Rootnode
            StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> handler = defaultArmoryFactory.armoryStrategyHandler();
            handler.apply(ArmoryCommandEntity.
                            builder()
                            .aiAgentConfigTableVO(table)
                            .build(),
                    new DefaultArmoryFactory.DynamicContext()
            );

        }


    }



}
