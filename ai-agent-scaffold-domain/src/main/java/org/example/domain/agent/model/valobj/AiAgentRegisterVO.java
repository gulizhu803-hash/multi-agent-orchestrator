package org.example.domain.agent.model.valobj;

import com.google.adk.runner.InMemoryRunner;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAgentRegisterVO {

    private String appName;
    private String agentName;
    private String agentId;
    private String agentType;
    /**
     * 智能体描述
     */
    private String agentDesc;
    /**
     * 智能体执行对象
     */
    private InMemoryRunner runner;

}
