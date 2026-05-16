package org.example.domain.agent.service.armory.assembly;

import org.example.domain.agent.model.valobj.AgentConfigVO;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;

public interface IConfigAssembler {
    String toYaml(AgentConfigVO config);
    AiAgentConfigTableVO toTableVO(AgentConfigVO config);
    AgentConfigVO fromTableVO(AiAgentConfigTableVO vo);
}
