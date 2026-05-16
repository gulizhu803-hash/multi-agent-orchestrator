package org.example.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.domain.agent.model.valobj.AgentConfigVO;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigSaveCommandEntity {
    private AgentConfigVO agentConfig;
}
