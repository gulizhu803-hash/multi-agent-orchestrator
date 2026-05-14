package org.example.api.dto;

import lombok.Data;

@Data
public class CreateSessionRequestDTO {
    private String userId;
    private String agentId;

}
