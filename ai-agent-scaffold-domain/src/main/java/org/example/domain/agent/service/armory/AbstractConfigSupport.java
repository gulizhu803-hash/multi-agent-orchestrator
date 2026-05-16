package org.example.domain.agent.service.armory;

import org.example.domain.agent.model.entity.ConfigSaveCommandEntity;
import org.example.domain.agent.service.armory.tree.AbstractMultiThreadStrategyRouter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractConfigSupport extends AbstractMultiThreadStrategyRouter<ConfigSaveCommandEntity, ConfigFactory.DynamicContext, String> {

    @Override
    protected void multiThread(ConfigSaveCommandEntity requestParameter, ConfigFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
    }
}
