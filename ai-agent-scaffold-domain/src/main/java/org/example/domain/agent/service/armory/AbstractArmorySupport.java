package org.example.domain.agent.service.armory;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractArmorySupport extends AbstractMultiThreadStrategyRouter<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> {
    protected final Logger log = LoggerFactory.getLogger(AbstractArmorySupport.class);


    /**
     * Spring 应用上下文，用于获取 Bean 实例
     */
    @Resource
    protected ApplicationContext applicationContext;
    @Override
    protected void multiThread(ArmoryCommandEntity requestParameter, DefaultArmoryFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }
    protected <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }
}
