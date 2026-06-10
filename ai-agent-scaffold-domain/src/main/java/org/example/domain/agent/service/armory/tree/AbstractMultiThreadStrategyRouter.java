package org.example.domain.agent.service.armory.tree;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public abstract class AbstractMultiThreadStrategyRouter<T, D, R> implements StrategyHandler<T, D, R> {

    protected StrategyHandler<T, D, R> defaultStrategyHandler = new StrategyHandler<T, D, R>() {
        @Override
        public R apply(T requestParameter, D dynamicContext) throws Exception {
            return null;
        }
    };

    protected abstract R doApply(T requestParameter, D dynamicContext) throws Exception;

    public abstract StrategyHandler<T, D, R> get(T requestParameter, D dynamicContext) throws Exception;

    protected abstract void multiThread(T requestParameter, D dynamicContext) throws ExecutionException, InterruptedException, TimeoutException;

    protected R router(T requestParameter, D dynamicContext) throws Exception {
        StrategyHandler<T, D, R> handler = get(requestParameter, dynamicContext);
        if (handler != null) {
            return handler.apply(requestParameter, dynamicContext);
        }
        return doApply(requestParameter, dynamicContext);
    }

    @Override
    public R apply(T requestParameter, D dynamicContext) throws Exception {
        return doApply(requestParameter, dynamicContext);
    }
}
