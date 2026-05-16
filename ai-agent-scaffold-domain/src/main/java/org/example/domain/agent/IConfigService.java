package org.example.domain.agent;

import org.example.domain.agent.model.entity.ConfigSaveCommandEntity;
import org.example.domain.agent.model.valobj.AgentConfigVO;

import java.util.List;

/**
 * 智能体配置服务接口
 * <p>
 * 提供智能体配置的保存、查询和删除功能。
 */
public interface IConfigService {

    /**
     * 保存智能体配置
     *
     * @param command 配置保存命令实体，包含需要保存的配置信息
     * @return 保存成功后的配置ID或相关标识
     * @throws Exception 保存过程中发生的异常
     */
    String saveAgentConfig(ConfigSaveCommandEntity command) throws Exception;

    /**
     * 获取所有智能体配置列表
     *
     * @return 智能体配置对象列表
     */
    List<AgentConfigVO> listAgentConfigs();

    /**
     * 删除指定应用的智能体配置
     *
     * @param appName 应用名称，用于标识需要删除的配置
     * @throws Exception 删除过程中发生的异常
     */
    void deleteAgentConfig(String appName) throws Exception;
}
