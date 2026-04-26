package org.example.domain.agent;

import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;

import java.util.ArrayList;
import java.util.List;

public interface IArmoryService {
    void acceptArmoryAgents(List<AiAgentConfigTableVO> tables) throws Exception;
}
