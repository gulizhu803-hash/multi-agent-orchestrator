package org.example.domain.agent.service.armory.assembly;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.example.domain.agent.model.valobj.AgentConfigVO;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ConfigAssembler implements IConfigAssembler {

    private final ObjectMapper yamlMapper;

    public ConfigAssembler() {
        YAMLFactory yamlFactory = new YAMLFactory()
                .configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true);
        this.yamlMapper = new ObjectMapper(yamlFactory);
    }

    @Override
    public String toYaml(AgentConfigVO config) {
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> ai = new LinkedHashMap<>();
        Map<String, Object> agent = new LinkedHashMap<>();
        Map<String, Object> configMap = new LinkedHashMap<>();
        Map<String, Object> tables = new LinkedHashMap<>();

        Map<String, Object> tableEntry = new LinkedHashMap<>();
        tableEntry.put("app-name", config.getAppName());

        Map<String, Object> agentMeta = new LinkedHashMap<>();
        agentMeta.put("agent-id", config.getAgentId());
        agentMeta.put("agent-name", config.getAgentName());
        agentMeta.put("agent-desc", config.getAgentDesc());
        tableEntry.put("agent", agentMeta);

        Map<String, Object> module = new LinkedHashMap<>();
        module.put("ai-api", buildAiApiMap(config));
        module.put("chat-model", buildChatModelMap(config));

        if (config.getAgents() != null) {
            List<Map<String, Object>> agentList = config.getAgents().stream().map(ad -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", ad.getName());
                entry.put("instruction", ad.getInstruction());
                entry.put("description", ad.getDescription());
                if (ad.getOutputKey() != null) entry.put("output-key", ad.getOutputKey());
                return entry;
            }).collect(Collectors.toList());
            module.put("agents", agentList);
        }

        if (config.getAgentWorkflows() != null && !config.getAgentWorkflows().isEmpty()) {
            List<Map<String, Object>> workflowList = config.getAgentWorkflows().stream().map(wf -> {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("type", wf.getType());
                entry.put("name", wf.getName());
                entry.put("sub-agents", wf.getSubAgents());
                entry.put("description", wf.getDescription());
                entry.put("max-iterations", wf.getMaxIterations() != null ? wf.getMaxIterations() : 3);
                return entry;
            }).collect(Collectors.toList());
            module.put("agent-workflows", workflowList);
        }

        Map<String, Object> runner = new LinkedHashMap<>();
        runner.put("agent-name", config.getRunnerAgentName());
        if (config.getPluginNameList() != null && !config.getPluginNameList().isEmpty()) {
            runner.put("plugin-name-list", config.getPluginNameList());
        }
        module.put("runner", runner);

        tableEntry.put("module", module);
        tables.put(config.getAppName(), tableEntry);
        configMap.put("tables", tables);
        agent.put("config", configMap);
        ai.put("agent", agent);
        root.put("ai", ai);

        try {
            return yamlMapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("YAML 序列化失败", e);
        }
    }

    private Map<String, Object> buildAiApiMap(AgentConfigVO config) {
        Map<String, Object> aiApi = new LinkedHashMap<>();
        aiApi.put("base-url", config.getBaseUrl());
        aiApi.put("api-key", config.getApiKey());
        aiApi.put("completions-path", config.getCompletionsPath() != null ? config.getCompletionsPath() : "v1/chat/completions");
        aiApi.put("embeddings-path", config.getEmbeddingsPath() != null ? config.getEmbeddingsPath() : "v1/embeddings");
        return aiApi;
    }

    private Map<String, Object> buildChatModelMap(AgentConfigVO config) {
        Map<String, Object> chatModel = new LinkedHashMap<>();
        chatModel.put("model", config.getModel());
        if (config.getToolMcpList() != null && !config.getToolMcpList().isEmpty()) {
            chatModel.put("tool-mcp-list", config.getToolMcpList().stream().map(this::buildMcpMap).collect(Collectors.toList()));
        }
        return chatModel;
    }

    private Map<String, Object> buildMcpMap(AgentConfigVO.McpToolConfig mcp) {
        Map<String, Object> mcpEntry = new LinkedHashMap<>();
        Map<String, Object> typeEntry = new LinkedHashMap<>();
        typeEntry.put("name", mcp.getName());

        switch (mcp.getType()) {
            case "sse":
                typeEntry.put("base-uri", mcp.getBaseUri());
                typeEntry.put("sse-endpoint", mcp.getSseEndpoint());
                typeEntry.put("request-timeout", mcp.getRequestTimeout() != null ? mcp.getRequestTimeout() : 3000);
                mcpEntry.put("sse", typeEntry);
                break;
            case "stdio": {
                Map<String, Object> serverParams = new LinkedHashMap<>();
                serverParams.put("command", mcp.getCommand());
                serverParams.put("args", mcp.getArgs());
                serverParams.put("env", mcp.getEnv());
                typeEntry.put("request-timeout", mcp.getRequestTimeout() != null ? mcp.getRequestTimeout() : 3000);
                typeEntry.put("server-parameters", serverParams);
                mcpEntry.put("stdio", typeEntry);
                break;
            }
            case "local":
                mcpEntry.put("local", typeEntry);
                break;
        }
        return mcpEntry;
    }

    @Override
    public AiAgentConfigTableVO toTableVO(AgentConfigVO config) {
        AiAgentConfigTableVO vo = new AiAgentConfigTableVO();
        vo.setAppName(config.getAppName());

        AiAgentConfigTableVO.Agent agent = new AiAgentConfigTableVO.Agent();
        agent.setAgentId(config.getAgentId());
        agent.setAgentName(config.getAgentName());
        agent.setAgentDesc(config.getAgentDesc());
        vo.setAgent(agent);

        AiAgentConfigTableVO.Module module = new AiAgentConfigTableVO.Module();

        AiAgentConfigTableVO.Module.AiApi aiApi = new AiAgentConfigTableVO.Module.AiApi();
        aiApi.setBaseUrl(config.getBaseUrl());
        aiApi.setApiKey(config.getApiKey());
        aiApi.setCompletionsPath(config.getCompletionsPath() != null ? config.getCompletionsPath() : "v1/chat/completions");
        aiApi.setEmbeddingsPath(config.getEmbeddingsPath() != null ? config.getEmbeddingsPath() : "v1/embeddings");
        module.setAiApi(aiApi);

        AiAgentConfigTableVO.Module.ChatModel chatModel = new AiAgentConfigTableVO.Module.ChatModel();
        chatModel.setModel(config.getModel());
        if (config.getToolMcpList() != null) {
            chatModel.setToolMcpList(config.getToolMcpList().stream().map(this::buildToolMcp).collect(Collectors.toList()));
        }
        module.setChatModel(chatModel);

        if (config.getAgents() != null) {
            module.setAgents(config.getAgents().stream().map(ad -> {
                AiAgentConfigTableVO.Module.Agent agentDef = new AiAgentConfigTableVO.Module.Agent();
                agentDef.setName(ad.getName());
                agentDef.setInstruction(ad.getInstruction());
                agentDef.setDescription(ad.getDescription());
                agentDef.setOutputKey(ad.getOutputKey());
                return agentDef;
            }).collect(Collectors.toList()));
        }

        if (config.getAgentWorkflows() != null) {
            module.setAgentWorkflows(config.getAgentWorkflows().stream().map(wf -> {
                AiAgentConfigTableVO.Module.AgentWorkflow workflow = new AiAgentConfigTableVO.Module.AgentWorkflow();
                workflow.setType(wf.getType());
                workflow.setName(wf.getName());
                workflow.setSubAgents(wf.getSubAgents());
                workflow.setDescription(wf.getDescription());
                workflow.setMaxIterations(wf.getMaxIterations() != null ? wf.getMaxIterations() : 3);
                return workflow;
            }).collect(Collectors.toList()));
        }

        AiAgentConfigTableVO.Module.Runner runner = new AiAgentConfigTableVO.Module.Runner();
        runner.setAgentName(config.getRunnerAgentName());
        runner.setPluginNameList(config.getPluginNameList());
        module.setRunner(runner);

        vo.setModule(module);
        return vo;
    }

    private AiAgentConfigTableVO.Module.ChatModel.ToolMcp buildToolMcp(AgentConfigVO.McpToolConfig mcp) {
        AiAgentConfigTableVO.Module.ChatModel.ToolMcp toolMcp = new AiAgentConfigTableVO.Module.ChatModel.ToolMcp();
        switch (mcp.getType()) {
            case "sse": {
                AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters sse =
                        new AiAgentConfigTableVO.Module.ChatModel.ToolMcp.SSEServerParameters();
                sse.setName(mcp.getName());
                sse.setBaseUri(mcp.getBaseUri());
                sse.setSseEndpoint(mcp.getSseEndpoint());
                sse.setRequestTimeout(mcp.getRequestTimeout());
                toolMcp.setSse(sse);
                break;
            }
            case "stdio": {
                AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters stdio =
                        new AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters();
                stdio.setName(mcp.getName());
                stdio.setRequestTimeout(mcp.getRequestTimeout());
                if (mcp.getCommand() != null) {
                    AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters sp =
                            new AiAgentConfigTableVO.Module.ChatModel.ToolMcp.StdioServerParameters.ServerParameters();
                    sp.setCommand(mcp.getCommand());
                    sp.setArgs(mcp.getArgs());
                    sp.setEnv(mcp.getEnv());
                    stdio.setServerParameters(sp);
                }
                toolMcp.setStdio(stdio);
                break;
            }
            case "local": {
                AiAgentConfigTableVO.Module.ChatModel.ToolMcp.LocalParameters local =
                        new AiAgentConfigTableVO.Module.ChatModel.ToolMcp.LocalParameters();
                local.setName(mcp.getName());
                toolMcp.setLocal(local);
                break;
            }
        }
        return toolMcp;
    }

    @Override
    public AgentConfigVO fromTableVO(AiAgentConfigTableVO vo) {
        AgentConfigVO config = new AgentConfigVO();
        config.setAppName(vo.getAppName());

        if (vo.getAgent() != null) {
            config.setAgentId(vo.getAgent().getAgentId());
            config.setAgentName(vo.getAgent().getAgentName());
            config.setAgentDesc(vo.getAgent().getAgentDesc());
        }
        if (vo.getModule() != null) {
            AiAgentConfigTableVO.Module module = vo.getModule();
            if (module.getAiApi() != null) {
                config.setBaseUrl(module.getAiApi().getBaseUrl());
                config.setApiKey(module.getAiApi().getApiKey());
                config.setCompletionsPath(module.getAiApi().getCompletionsPath());
                config.setEmbeddingsPath(module.getAiApi().getEmbeddingsPath());
            }
            if (module.getChatModel() != null) {
                config.setModel(module.getChatModel().getModel());
            }
            if (module.getAgents() != null) {
                config.setAgents(module.getAgents().stream().map(a -> {
                    AgentConfigVO.AgentDefinition ad = new AgentConfigVO.AgentDefinition();
                    ad.setName(a.getName());
                    ad.setInstruction(a.getInstruction());
                    ad.setDescription(a.getDescription());
                    ad.setOutputKey(a.getOutputKey());
                    return ad;
                }).collect(Collectors.toList()));
            }
            if (module.getRunner() != null) {
                config.setRunnerAgentName(module.getRunner().getAgentName());
                config.setPluginNameList(module.getRunner().getPluginNameList());
            }
        }
        return config;
    }
}
