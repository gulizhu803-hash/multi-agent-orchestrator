package org.example.api;

import org.example.api.dto.AgentConfigDTO;
import org.example.api.response.Response;
import java.util.List;

public interface IAgentConfigApi {
    Response<String> saveAgentConfig(AgentConfigDTO config);
    Response<List<AgentConfigDTO>> listAgentConfigs();
    Response<Void> deleteAgentConfig(String appName);
}
