package org.example.trigger.http;

import com.google.adk.events.Event;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.example.api.IAgentService;
import org.example.api.dto.*;
import org.example.api.response.Response;
import org.example.domain.agent.IChatService;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/api/v1/")
@RestController
@CrossOrigin(origins = "*")
public class AgentServiceController implements IAgentService {
    @Resource
    private IChatService chatService;
    @Autowired
    private RestClient.Builder builder;

    /**
     * 查询智能体配置列表
     * @return 智能体配置响应列表
     */
    @RequestMapping(value = "query_ai_agent_config_list", method = RequestMethod.GET)
    @Override
    public Response<List<AiAgentConfigResponseDTO>>  queryAiAgentConfigList(){

        try {
            log.info("查询智能体配置列表");

            List<AiAgentConfigTableVO.Agent> agents = chatService.queryAiAgentConfigList();

            //代替循环
            List<AiAgentConfigResponseDTO> responseDTOS = agents.stream().map(agent -> {
                AiAgentConfigResponseDTO responseDTO = new AiAgentConfigResponseDTO();
                responseDTO.setAgentId(agent.getAgentId());
                responseDTO.setAgentDesc(agent.getAgentDesc());
                responseDTO.setAgentName(agent.getAgentName());
                return responseDTO;
            }).collect(Collectors.toList());

            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTOS)
                    .build();
        }catch (AppException e) {
            log.error("查询智能体配置列表异常", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("查询智能体配置列表失败", e);
            return Response.<List<AiAgentConfigResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }


    }


    /**
     * 创建会话
     * @param requestDTO 创建会话请求参数
     * @return 创建会话响应结果，包含会话ID等信息
     */
    @RequestMapping(value = "create_session", method = RequestMethod.GET)
//    @Override
    public Response<CreateSessionResponseDTO> createSession( CreateSessionRequestDTO requestDTO){
        try {
            // 参数校验
            if (requestDTO == null || requestDTO.getAgentId() == null || requestDTO.getAgentId().trim().isEmpty()) {
                log.warn("创建会话失败：agentId 为空");
                return Response.<CreateSessionResponseDTO>builder()
                        .code(ResponseCode.MISSING_REQUIRED_PARAM.getCode())
                        .info("agentId 不能为空")
                        .build();
            }
            if (requestDTO.getUserId() == null || requestDTO.getUserId().trim().isEmpty()) {
                log.warn("创建会话失败：userId 为空");
                return Response.<CreateSessionResponseDTO>builder()
                        .code(ResponseCode.MISSING_REQUIRED_PARAM.getCode())
                        .info("userId 不能为空")
                        .build();
            }

            log.info("创建会话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            String sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            CreateSessionResponseDTO responseDTO = new CreateSessionResponseDTO();
            responseDTO.setSessionId(sessionId);

            return Response.<CreateSessionResponseDTO>builder().
                    code(ResponseCode.SUCCESS.getCode()).
                    info(ResponseCode.SUCCESS.getInfo()).
                    data(responseDTO).
                    build();
        } catch (AppException e) {
            log.error("创建会话业务异常", e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("创建会话失败 agentId:{} userId:{}", requestDTO != null ? requestDTO.getAgentId() : "null", requestDTO != null ? requestDTO.getUserId() : "null", e);
            return Response.<CreateSessionResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }

    }

    /**
     * 普通聊天对话（非流式）
     * @param requestDTO 聊天请求参数，包含用户消息、会话ID等
     * @return 聊天响应结果，包含AI回复内容
     */
    @RequestMapping(value = "chat", method = RequestMethod.POST)
//    @Override
    public Response<ChatResponseDTO> chat(@RequestBody ChatRequestDTO requestDTO) {
        try {
            log.info("智能体对话 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId());
            String sessionId = requestDTO.getSessionId();
            if (sessionId == null || sessionId.isEmpty()) {
                sessionId = chatService.createSession(requestDTO.getAgentId(), requestDTO.getUserId());
            }

            List<String> messages = chatService.handleMessage(requestDTO.getAgentId(), requestDTO.getUserId(), sessionId, requestDTO.getMessage());

            ChatResponseDTO responseDTO = new ChatResponseDTO();
            // 将消息列表合并为单个字符串，使用换行符分隔，并设置到响应对象中
            responseDTO.setContent(String.join("\n", messages));

            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();
        } catch (AppException e) {
            log.error("智能体对话异常", e);
            return Response.<ChatResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("智能体对话败 agentId:{} userId:{}", requestDTO.getAgentId(), requestDTO.getUserId(), e);
            return Response.<ChatResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
    /**
     * 流式聊天对话
     * @param requestDTO 聊天请求参数，包含用户消息、会话ID等
     * @return 流式响应发射器，用于实时推送AI回复内容
     */
    @RequestMapping(value = "chat_stream", method = RequestMethod.POST)
    @Override
    public ResponseBodyEmitter chatStream(@RequestBody ChatRequestDTO requestDTO) {
        ResponseBodyEmitter emitter = new ResponseBodyEmitter(3 * 60 * 1000L);
        try {
            log.info("流式对话 agentId:{} userId:{} sessionId:{} message:{}", requestDTO.getAgentId(), requestDTO.getUserId(), requestDTO.getSessionId(), requestDTO.getMessage());
            chatService.handleMessageStream(requestDTO.getAgentId(), requestDTO.getUserId(), requestDTO.getSessionId(), requestDTO.getMessage())
                    .subscribe(
                            event -> {
                                try {
                                    emitter.send(event.stringifyContent());
                                } catch (Exception e) {
                                    log.error("流式对话发送失败", e);
                                    emitter.completeWithError(e);
                                }
                            },
                            emitter::completeWithError,
                            emitter::complete
                    );
        } catch (Exception e) {
            log.error("流式对话失败", e);
            emitter.completeWithError(e);
        }
        return emitter;
    }


}
