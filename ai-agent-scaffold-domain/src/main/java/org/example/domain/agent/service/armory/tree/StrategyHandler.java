package org.example.domain.agent.service.armory.tree;

public interface StrategyHandler<T, D, R> {

    @SuppressWarnings({"rawtypes", "unchecked"})
    StrategyHandler DEFAULT = (StrategyHandler) (requestParameter, dynamicContext) -> null;

    R apply(T requestParameter, D dynamicContext) throws Exception;

}
