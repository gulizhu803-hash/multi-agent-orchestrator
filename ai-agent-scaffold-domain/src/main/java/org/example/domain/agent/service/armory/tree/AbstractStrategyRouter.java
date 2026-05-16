package org.example.domain.agent.service.armory.tree;

public abstract class AbstractStrategyRouter<T, D, R> implements StrategyMapper<T, D, R>, StrategyHandler<T, D, R> {

    protected StrategyHandler<T, D, R> defaultStrategyHandler;

    public AbstractStrategyRouter() {
        this.defaultStrategyHandler = StrategyHandler.DEFAULT;
    }

    public R router(T requestParameter, D dynamicContext) throws Exception {
        StrategyHandler<T, D, R> handler = this.get(requestParameter, dynamicContext);
        if (handler != null) {
            return handler.apply(requestParameter, dynamicContext);
        }
        return this.defaultStrategyHandler.apply(requestParameter, dynamicContext);
    }

    public StrategyHandler<T, D, R> getDefaultStrategyHandler() {
        return defaultStrategyHandler;
    }

    public void setDefaultStrategyHandler(StrategyHandler<T, D, R> defaultStrategyHandler) {
        this.defaultStrategyHandler = defaultStrategyHandler;
    }

}
