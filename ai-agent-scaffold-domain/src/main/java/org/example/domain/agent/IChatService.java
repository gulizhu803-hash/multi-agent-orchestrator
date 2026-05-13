package org.example.domain.agent;

import com.google.adk.events.Event;
import io.reactivex.rxjava3.core.Flowable;
import org.example.domain.agent.model.entity.ChatCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;

import java.util.List;

public interface IChatService {
    /**
     * 查询AI智能体配置列表
     *
     * @return AI智能体配置列表
     */
    List<AiAgentConfigTableVO.Agent> queryAiAgentConfigList();

    /**
     * 创建会话
     *
     * @param agentId 智能体ID
     * @param userId  用户ID
     * @return 会话ID
     */
    String  createSession(String agentId, String userId);


    /**
     * 处理消息（自动管理会话）
     *
     * @param agentId 智能体ID
     * @param userId  用户ID
     * @param message 用户消息
     * @return AI回复消息列表
     */
    List<String> handleMessage(String agentId, String userId, String message);



    /**
     * 处理消息（指定会话）
     *
     * @param agentId   智能体ID
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return AI回复消息列表
     */

    List<String> handleMessage(String agentId,String userId,String sessionId,String message);

    /**
     * 处理消息流（流式响应）
     *
     * @param agentId   智能体ID
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param message   用户消息
     * @return AI回复事件流
     */

    Flowable<Event> handleMessageStream(String agentId,String userId,String sessionId, String message);

    /**
     * 处理消息（基于命令实体）
     *
     * @param chatCommandEntity 聊天命令实体
     * @return AI回复消息列表
     */
    List<String> handleMessage(ChatCommandEntity chatCommandEntity);

}
