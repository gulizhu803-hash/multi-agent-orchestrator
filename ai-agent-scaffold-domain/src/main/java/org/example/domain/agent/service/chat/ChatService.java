package org.example.domain.agent.service.chat;

import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import guru.nidi.graphviz.attribute.LinkAttr;
import io.reactivex.rxjava3.core.Flowable;
import org.apache.catalina.Context;
import org.example.domain.agent.IChatService;
import org.example.domain.agent.model.entity.ChatCommandEntity;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService implements IChatService {

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    private  final Map<String,String> userSessions = new ConcurrentHashMap<>();

    @Override
    public List<AiAgentConfigTableVO.Agent> queryAiAgentConfigList() {
        Map<String, AiAgentConfigTableVO> tables =aiAgentAutoConfigProperties.getTables();
//        系统的配置文件（如 yml 文件）中，把所有已经定义好的 AI 智能体（Agent）提取出来
        List<AiAgentConfigTableVO.Agent> agents = new ArrayList<>();
        if (null!=tables){
            for (AiAgentConfigTableVO tableVO : tables.values()) {
                AiAgentConfigTableVO.Agent agent = tableVO.getAgent();
                if (null != agent) {
                    agents.add(agent);
                } else {
                    throw new RuntimeException("Agent is null");
                }
            }
        }
        return agents;
    }

    @Override
    public String createSession(String agentId, String userId) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);

        if (null == aiAgentRegisterVO) {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        String appName = aiAgentRegisterVO.getAppName();
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();

        return userSessions.computeIfAbsent(userId, uid -> {
            Session session = runner.sessionService().createSession(appName, uid)
                    .blockingGet();
            return session.id();
        });
    }

    /**
     * 处理消息---主要职责是获取sessionId
     * @param agentId 智能体ID
     * @param userId  用户ID
     * @param message 用户消息
     * @return
     */
    @Override
    public List<String> handleMessage(String agentId, String userId, String message) {

        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);
        if(null  == aiAgentRegisterVO)
        {
            throw new AppException(ResponseCode.E0001.getCode());
        }
        String sessionId = createSession(agentId, userId);
        /**
         * 方法重载--创建会话，传入sessionId给下一个方法
         * 这样写保证了核心代码只有一份。以后如果你想修改 AI 回复的格式，只需要改 4 参数的那个方法，这个 3 参数的方法会自动受益。
         */
        return handleMessage(agentId, userId, sessionId, message);
    }

    @Override
    public List<String> handleMessage(String agentId, String userId, String sessionId, String message) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);
        if (null == aiAgentRegisterVO){
            throw new AppException(ResponseCode.E0001.getCode());
        }
//        Context context = fromP
        Content content =Content.fromParts(Part.fromText(message));

        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        Flowable<Event> events = runner.runAsync(userId,sessionId, content);
        List<String> output = new ArrayList<>();

        events.blockingForEach(event -> output.add(event.stringifyContent()));
        return output;
    }

    @Override
    public Flowable<Event> handleMessageStream(String agentId, String userId, String sessionId, String message) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(agentId);
        if (null == aiAgentRegisterVO)
        {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        Content userMessage = Content.fromParts(Part.fromText(message));
        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        return runner.runAsync(userId, sessionId, userMessage);

    }

    @Override
    public List<String> handleMessage(ChatCommandEntity chatCommandEntity) {
        AiAgentRegisterVO aiAgentRegisterVO = defaultArmoryFactory.getAiAgentRegisterVO(chatCommandEntity.getAgentId());

        if (null == aiAgentRegisterVO)
        {
            throw new AppException(ResponseCode.E0001.getCode());
        }

        List<Part> parts = new ArrayList<>();

        List<ChatCommandEntity.Content.Text> texts = chatCommandEntity.getTexts();
        if (null != texts  && !texts.isEmpty())
        {
            for (ChatCommandEntity.Content.Text text : texts)
            {
                parts.add(Part.fromText(text.getMessage()));
            }
        }

        List<ChatCommandEntity.Content.File> files = chatCommandEntity.getFiles();
        if (null != files && !files.isEmpty()){
            for (ChatCommandEntity.Content.File file : files){
                parts.add(Part.fromUri(file.getFileUri(), file.getMimeType()));
            }
        }

        List<ChatCommandEntity.Content.InlineData> inlineData = chatCommandEntity.getInlineDatas();
        if (null != inlineData && !inlineData.isEmpty())
        {
            for (ChatCommandEntity.Content.InlineData data : inlineData)
            {
                parts.add(Part.fromBytes(data.getBytes(), data.getMimeType()));
            }
        }
        Content content = Content.builder().role("user").parts(parts).build();

        InMemoryRunner runner = aiAgentRegisterVO.getRunner();
        Flowable<Event> events = runner.runAsync(chatCommandEntity.getUserId(), chatCommandEntity.getSessionId(), content);
        List<String> output = new ArrayList<>();

        events.blockingForEach(event -> output.add(event.stringifyContent()));

        return output;


    }
}
