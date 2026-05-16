package org.example.domain.agent.service.armory.tree;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractMultiThreadStrategyRouter<T, D, R> implements StrategyMapper<T, D, R>, StrategyHandler<T, D, R> {

    protected StrategyHandler<T, D, R> defaultStrategyHandler;

    public AbstractMultiThreadStrategyRouter() {
        this.defaultStrategyHandler = StrategyHandler.DEFAULT;
    }

    public R router(T requestParameter, D dynamicContext) throws Exception {
        StrategyHandler<T, D, R> handler = this.get(requestParameter, dynamicContext);
        if (handler != null) {
            return handler.apply(requestParameter, dynamicContext);
        }
        return this.defaultStrategyHandler.apply(requestParameter, dynamicContext);
    }

    @Override
    public R apply(T requestParameter, D dynamicContext) throws Exception {
        this.multiThread(requestParameter, dynamicContext);
        return this.doApply(requestParameter, dynamicContext);
    }

    protected abstract void multiThread(T requestParameter, D dynamicContext) throws ExecutionException, InterruptedException, TimeoutException;

    protected abstract R doApply(T requestParameter, D dynamicContext) throws Exception;

    public StrategyHandler<T, D, R> getDefaultStrategyHandler() {
        return defaultStrategyHandler;
    }

    public void setDefaultStrategyHandler(StrategyHandler<T, D, R> defaultStrategyHandler) {
        this.defaultStrategyHandler = defaultStrategyHandler;
    }

}
