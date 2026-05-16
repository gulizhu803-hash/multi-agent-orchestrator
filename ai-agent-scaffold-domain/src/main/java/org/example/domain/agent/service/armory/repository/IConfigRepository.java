package org.example.domain.agent.service.armory.repository;

public interface IConfigRepository {
    String save(String appName, String yamlContent) throws Exception;
    void delete(String appName) throws Exception;
}
