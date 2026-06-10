package org.example.domain.agent.service.armory.tree;

public interface StrategyHandler<T, D, R> {

    R apply(T requestParameter, D dynamicContext) throws Exception;
}
