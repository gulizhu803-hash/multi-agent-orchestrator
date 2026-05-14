package org.example.api;

import org.example.api.dto.*;
import org.example.api.response.Response;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

public interface IAgentService {
    /**
     * 查询智能体配置列表
     * @return 智能体配置响应列表
     */
    Response<List<AiAgentConfigResponseDTO>>  queryAiAgentConfigList();


    /**
     * 创建会话
     * @param requestDTO 创建会话请求参数
     * @return 创建会话响应结果，包含会话ID等信息
     */
    Response<CreateSessionResponseDTO> createSession(CreateSessionRequestDTO requestDTO);

    /**
     * 普通聊天对话（非流式）
     * @param requestDTO 聊天请求参数，包含用户消息、会话ID等
     * @return 聊天响应结果，包含AI回复内容
     */
    Response<ChatResponseDTO> chat(ChatRequestDTO requestDTO);

    /**
     * 流式聊天对话
     * @param requestDTO 聊天请求参数，包含用户消息、会话ID等
     * @return 流式响应发射器，用于实时推送AI回复内容
     */
    ResponseBodyEmitter chatStream(ChatRequestDTO requestDTO);
}
