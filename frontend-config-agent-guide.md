# 前端配置智能体 + 后端生成 YAML 实现指导

## 整体思路

当前项目通过 **YAML 文件 → `@ConfigurationProperties` → `ArmoryService`** 在启动时自动装配智能体。要让前端动态配置智能体，完整的链路是：

```
前端配置表单 → 后端保存 YAML → 触发热装配（不重启）
```

## 方案设计（推荐方案：文件 + 热加载）

### 架构概览

```
┌─────────────────────┐      POST /api/v1/agent_config      ┌────────────────────────────┐
│   前端配置页面       │ ──────────────────────────────────→  │   AgentConfigController    │
│   (Agent Designer)  │                                      │   (新增)                   │
└─────────────────────┘                                      └───────────┬────────────────┘
                                                                        │
                                                                        ▼
                                                              ┌────────────────────────────┐
                                                              │   AgentConfigService       │
                                                              │   1. 校验参数               │
                                                              │   2. 序列化为 YAML          │
                                                              │   3. 保存到 resources/agent/ │
                                                              │   4. 调用热装配              │
                                                              └────────────────────────────┘
```

---

## 第一步：后端 — 新增 DTO

新建 `ai-agent-scaffold-api/src/main/java/org/example/api/dto/AgentConfigDTO.java`：

```java
package org.example.api.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 前端提交的智能体配置（对应 YAML 结构）
 */
@Data
public class AgentConfigDTO {
    // ======== 基本信息 ========
    private String appName;          // 应用/智能体标识名，如 "myAgent"
    private String agentId;          // 智能体 ID，如 "100004"
    private String agentName;        // 智能体展示名称
    private String agentDesc;        // 智能体描述

    // ======== LLM API 配置 ========
    private String baseUrl;          // API 地址
    private String apiKey;           // API Key
    private String completionsPath;  // 补全路径，默认 "v1/chat/completions"
    private String embeddingsPath;   // 嵌入路径，默认 "v1/embeddings"

    // ======== 模型配置 ========
    private String model;            // 模型名，如 "gpt-4.1"

    // ======== MCP 工具配置（可选） ========
    private List<McpToolConfig> toolMcpList;

    // ======== 智能体定义 ========
    private List<AgentDefinition> agents;

    // ======== 工作流配置（可选） ========
    private List<WorkflowConfig> agentWorkflows;

    // ======== Runner 配置 ========
    private String runnerAgentName;           // 运行入口 agent 名称
    private List<String> pluginNameList;      // 插件名列表

    // ======== 内部嵌套类 ========

    @Data
    public static class McpToolConfig {
        private String type;          // "sse" | "stdio" | "local"
        // SSE 参数
        private String name;
        private String baseUri;
        private String sseEndpoint;
        private Integer requestTimeout;
        // Stdio 参数
        private String command;
        private List<String> args;
        private Map<String, String> env;
        // Local 参数（只需 name）
    }

    @Data
    public static class AgentDefinition {
        private String name;
        private String instruction;
        private String description;
        private String outputKey;
    }

    @Data
    public static class WorkflowConfig {
        private String type;           // "loop" | "parallel" | "sequential"
        private String name;
        private List<String> subAgents;
        private String description;
        private Integer maxIterations;
    }
}
```

---

## 第二步：后端 — 新增配置保存 + 热装配服务

新建 `ai-agent-scaffold-domain/src/main/java/org/example/domain/agent/service/config/IAgentConfigService.java`：

```java
package org.example.domain.agent.service.config;

import org.example.api.dto.AgentConfigDTO;
import java.util.List;

public interface IAgentConfigService {
    /**
     * 保存一个新的智能体配置
     * @param config 前端提交的配置
     * @return 保存的文件名
     */
    String saveAgentConfig(AgentConfigDTO config) throws Exception;

    /**
     * 获取所有已配置的智能体列表
     */
    List<AgentConfigDTO> listAgentConfigs();

    /**
     * 删除一个智能体配置
     */
    void deleteAgentConfig(String appName) throws Exception;
}
```

实现类 `ai-agent-scaffold-domain/src/main/java/org/example/domain/agent/service/config/AgentConfigService.java`：

```java
package org.example.domain.agent.service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import lombok.extern.slf4j.Slf4j;
import org.example.api.dto.AgentConfigDTO;
import org.example.domain.agent.model.valobj.AiAgentConfigTableVO;
import org.example.domain.agent.model.valobj.AiAgentRegisterVO;
import org.example.domain.agent.model.valobj.properties.AiAgentAutoConfigProperties;
import org.example.domain.agent.service.armory.factory.DefaultArmoryFactory;
import org.example.domain.agent.service.armory.tree.StrategyHandler;
import org.example.domain.agent.IArmoryService;
import org.example.domain.agent.model.entity.ArmoryCommandEntity;
import org.example.types.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AgentConfigService implements IAgentConfigService {

    /** 智能体 YAML 配置文件存放目录 */
    @Value("${agent.config.dir:classpath:agent/}")
    private String configDir;

    @Resource
    private IArmoryService armoryService;

    @Resource
    private DefaultArmoryFactory defaultArmoryFactory;

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;

    private final ObjectMapper yamlMapper;

    public AgentConfigService() {
        // 配置 YAML 序列化器：不自动关闭、保留换行、缩进
        YAMLFactory yamlFactory = new YAMLFactory()
                .configure(YAMLGenerator.Feature.MINIMUM_QUOTES, true)
                .configure(YAMLGenerator.Feature.INDENT_ARRAYS, true)
                .configure(YAMLGenerator.Feature.ALWAYS_QUOTE_NUMBERS, false);
        this.yamlMapper = new ObjectMapper(yamlFactory);
    }

    @Override
    public String saveAgentConfig(AgentConfigDTO config) throws Exception {
        // 1. 参数校验
        Assert.hasText(config.getAppName(), "appName 不能为空");
        Assert.hasText(config.getAgentId(), "agentId 不能为空");
        Assert.hasText(config.getBaseUrl(), "baseUrl 不能为空");
        Assert.hasText(config.getApiKey(), "apiKey 不能为空");
        Assert.hasText(config.getModel(), "model 不能为空");
        Assert.notEmpty(config.getAgents(), "至少需要一个 agent 定义");

        // 2. DTO → YAML 字符串
        String yamlContent = convertDtoToYaml(config);

        // 3. 确定保存路径
        //    开发环境保存到 resources/agent/，生产环境可配置为外部路径
        String filename = config.getAppName() + ".yml";
        // 优先存到外部可写目录（如 data/agent-configs/）
        Path outputDir = Paths.get("data", "agent-configs");
        Files.createDirectories(outputDir);
        Path outputFile = outputDir.resolve(filename);

        // 4. 写入 YAML 文件
        Files.writeString(outputFile, yamlContent, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("智能体配置文件已保存: {}", outputFile.toAbsolutePath());

        // 5. 热装配：直接调用 armory 装配管线
        hotReloadAgent(config);

        return filename;
    }

    /**
     * 将前端 DTO 转换为 AiAgentConfigTableVO 并触发装配
     */
    private void hotReloadAgent(AgentConfigDTO config) throws Exception {
        // 构建 AiAgentConfigTableVO（与 YAML 反序列化产出的结构一致）
        AiAgentConfigTableVO tableVO = buildTableVO(config);

        // 直接调用 armory 的装配管线
        StrategyHandler<ArmoryCommandEntity, DefaultArmoryFactory.DynamicContext, AiAgentRegisterVO> handler =
                defaultArmoryFactory.armoryStrategyHandler();
        handler.apply(
                ArmoryCommandEntity.builder().aiAgentConfigTableVO(tableVO).build(),
                new DefaultArmoryFactory.DynamicContext()
        );

        log.info("智能体 {} 热装配完成", config.getAppName());
    }

    @Override
    public List<AgentConfigDTO> listAgentConfigs() {
        // 从 AiAgentAutoConfigProperties 中读取已装配的配置
        if (aiAgentAutoConfigProperties.getTables() == null) {
            return Collections.emptyList();
        }
        return aiAgentAutoConfigProperties.getTables().values().stream()
                .map(this::convertToConfigDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAgentConfig(String appName) throws Exception {
        Path outputFile = Paths.get("data", "agent-configs", appName + ".yml");
        Files.deleteIfExists(outputFile);
        // 注：从已注册的 bean 中移除较为复杂，此处略，推荐重启应用
        log.warn("配置文件 {} 已删除，需重启应用以完全卸载智能体", outputFile.toAbsolutePath());
    }

    // ========== 私有辅助方法 ==========

    private String convertDtoToYaml(AgentConfigDTO dto) {
        // 构建符合 YAML 结构的 Map
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> ai = new LinkedHashMap<>();
        Map<String, Object> agent = new LinkedHashMap<>();
        Map<String, Object> config = new LinkedHashMap<>();
        Map<String, Object> tables = new LinkedHashMap<>();

        Map<String, Object> tableEntry = new LinkedHashMap<>();
        tableEntry.put("app-name", dto.getAppName());

        // agent 元信息
        Map<String, Object> agentMeta = new LinkedHashMap<>();
        agentMeta.put("agent-id", dto.getAgentId());
        agentMeta.put("agent-name", dto.getAgentName());
        agentMeta.put("agent-desc", dto.getAgentDesc());
        tableEntry.put("agent", agentMeta);

        // module
        Map<String, Object> module = new LinkedHashMap<>();
        Map<String, Object> aiApi = new LinkedHashMap<>();
        aiApi.put("base-url", dto.getBaseUrl());
        aiApi.put("api-key", dto.getApiKey());
        aiApi.put("completions-path", dto.getCompletionsPath() != null ? dto.getCompletionsPath() : "v1/chat/completions");
        aiApi.put("embeddings-path", dto.getEmbeddingsPath() != null ? dto.getEmbeddingsPath() : "v1/embeddings");
        module.put("ai-api", aiApi);

        Map<String, Object> chatModel = new LinkedHashMap<>();
        chatModel.put("model", dto.getModel());
        if (dto.getToolMcpList() != null && !dto.getToolMcpList().isEmpty()) {
            List<Map<String, Object>> mcpList = new ArrayList<>();
            for (AgentConfigDTO.McpToolConfig mcp : dto.getToolMcpList()) {
                Map<String, Object> mcpEntry = new LinkedHashMap<>();
                Map<String, Object> typeEntry = new LinkedHashMap<>();
                typeEntry.put("name", mcp.getName());
                if ("sse".equals(mcp.getType())) {
                    typeEntry.put("base-uri", mcp.getBaseUri());
                    typeEntry.put("sse-endpoint", mcp.getSseEndpoint());
                    typeEntry.put("request-timeout", mcp.getRequestTimeout() != null ? mcp.getRequestTimeout() : 3000);
                    mcpEntry.put("sse", typeEntry);
                } else if ("stdio".equals(mcp.getType())) {
                    Map<String, Object> serverParams = new LinkedHashMap<>();
                    serverParams.put("command", mcp.getCommand());
                    serverParams.put("args", mcp.getArgs());
                    serverParams.put("env", mcp.getEnv());
                    typeEntry.put("request-timeout", mcp.getRequestTimeout() != null ? mcp.getRequestTimeout() : 3000);
                    typeEntry.put("server-parameters", serverParams);
                    mcpEntry.put("stdio", typeEntry);
                } else if ("local".equals(mcp.getType())) {
                    mcpEntry.put("local", typeEntry);
                }
                mcpList.add(mcpEntry);
            }
            chatModel.put("tool-mcp-list", mcpList);
        }
        module.put("chat-model", chatModel);

        // agents
        if (dto.getAgents() != null) {
            List<Map<String, Object>> agentList = new ArrayList<>();
            for (AgentConfigDTO.AgentDefinition ad : dto.getAgents()) {
                Map<String, Object> agentEntry = new LinkedHashMap<>();
                agentEntry.put("name", ad.getName());
                agentEntry.put("instruction", ad.getInstruction());
                agentEntry.put("description", ad.getDescription());
                if (ad.getOutputKey() != null) {
                    agentEntry.put("output-key", ad.getOutputKey());
                }
                agentList.add(agentEntry);
            }
            module.put("agents", agentList);
        }

        // workflows
        if (dto.getAgentWorkflows() != null && !dto.getAgentWorkflows().isEmpty()) {
            List<Map<String, Object>> workflowList = new ArrayList<>();
            for (AgentConfigDTO.WorkflowConfig wf : dto.getAgentWorkflows()) {
                Map<String, Object> wfEntry = new LinkedHashMap<>();
                wfEntry.put("type", wf.getType());
                wfEntry.put("name", wf.getName());
                wfEntry.put("sub-agents", wf.getSubAgents());
                wfEntry.put("description", wf.getDescription());
                wfEntry.put("max-iterations", wf.getMaxIterations() != null ? wf.getMaxIterations() : 3);
                workflowList.add(wfEntry);
            }
            module.put("agent-workflows", workflowList);
        }

        // runner
        Map<String, Object> runner = new LinkedHashMap<>();
        runner.put("agent-name", dto.getRunnerAgentName());
        if (dto.getPluginNameList() != null && !dto.getPluginNameList().isEmpty()) {
            runner.put("plugin-name-list", dto.getPluginNameList());
        }
        module.put("runner", runner);

        tableEntry.put("module", module);
        tables.put(dto.getAppName(), tableEntry);
        config.put("tables", tables);
        agent.put("config", config);
        ai.put("agent", agent);
        root.put("ai", ai);

        try {
            return yamlMapper.writeValueAsString(root);
        } catch (IOException e) {
            throw new RuntimeException("YAML 序列化失败", e);
        }
    }

    private AiAgentConfigTableVO buildTableVO(AgentConfigDTO dto) {
        AiAgentConfigTableVO vo = new AiAgentConfigTableVO();
        vo.setAppName(dto.getAppName());

        AiAgentConfigTableVO.Agent agent = new AiAgentConfigTableVO.Agent();
        agent.setAgentId(dto.getAgentId());
        agent.setAgentName(dto.getAgentName());
        agent.setAgentDesc(dto.getAgentDesc());
        vo.setAgent(agent);

        AiAgentConfigTableVO.Module module = new AiAgentConfigTableVO.Module();

        // AiApi
        AiAgentConfigTableVO.Module.AiApi aiApi = new AiAgentConfigTableVO.Module.AiApi();
        aiApi.setBaseUrl(dto.getBaseUrl());
        aiApi.setApiKey(dto.getApiKey());
        aiApi.setCompletionsPath(dto.getCompletionsPath() != null ? dto.getCompletionsPath() : "v1/chat/completions");
        aiApi.setEmbeddingsPath(dto.getEmbeddingsPath() != null ? dto.getEmbeddingsPath() : "v1/embeddings");
        module.setAiApi(aiApi);

        // ChatModel
        AiAgentConfigTableVO.Module.ChatModel chatModel = new AiAgentConfigTableVO.Module.ChatModel();
        chatModel.setModel(dto.getModel());
        if (dto.getToolMcpList() != null) {
            chatModel.setToolMcpList(dto.getToolMcpList().stream().map(mcp -> {
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
            }).collect(Collectors.toList()));
        }
        module.setChatModel(chatModel);

        // Agents
        if (dto.getAgents() != null) {
            module.setAgents(dto.getAgents().stream().map(ad -> {
                AiAgentConfigTableVO.Module.Agent agentDef = new AiAgentConfigTableVO.Module.Agent();
                agentDef.setName(ad.getName());
                agentDef.setInstruction(ad.getInstruction());
                agentDef.setDescription(ad.getDescription());
                agentDef.setOutputKey(ad.getOutputKey());
                return agentDef;
            }).collect(Collectors.toList()));
        }

        // Workflows
        if (dto.getAgentWorkflows() != null) {
            module.setAgentWorkflows(dto.getAgentWorkflows().stream().map(wf -> {
                AiAgentConfigTableVO.Module.AgentWorkflow workflow = new AiAgentConfigTableVO.Module.AgentWorkflow();
                workflow.setType(wf.getType());
                workflow.setName(wf.getName());
                workflow.setSubAgents(wf.getSubAgents());
                workflow.setDescription(wf.getDescription());
                workflow.setMaxIterations(wf.getMaxIterations() != null ? wf.getMaxIterations() : 3);
                return workflow;
            }).collect(Collectors.toList()));
        }

        // Runner
        AiAgentConfigTableVO.Module.Runner runner = new AiAgentConfigTableVO.Module.Runner();
        runner.setAgentName(dto.getRunnerAgentName());
        runner.setPluginNameList(dto.getPluginNameList());
        module.setRunner(runner);
        vo.setModule(module);

        return vo;
    }

    private AgentConfigDTO convertToConfigDTO(AiAgentConfigTableVO vo) {
        AgentConfigDTO dto = new AgentConfigDTO();
        dto.setAppName(vo.getAppName());
        if (vo.getAgent() != null) {
            dto.setAgentId(vo.getAgent().getAgentId());
            dto.setAgentName(vo.getAgent().getAgentName());
            dto.setAgentDesc(vo.getAgent().getAgentDesc());
        }
        if (vo.getModule() != null) {
            AiAgentConfigTableVO.Module module = vo.getModule();
            if (module.getAiApi() != null) {
                dto.setBaseUrl(module.getAiApi().getBaseUrl());
                dto.setApiKey(module.getAiApi().getApiKey());
                dto.setCompletionsPath(module.getAiApi().getCompletionsPath());
                dto.setEmbeddingsPath(module.getAiApi().getEmbeddingsPath());
            }
            if (module.getChatModel() != null) {
                dto.setModel(module.getChatModel().getModel());
                // MCP 转换略（同理反向映射）
            }
            if (module.getAgents() != null) {
                dto.setAgents(module.getAgents().stream().map(a -> {
                    AgentConfigDTO.AgentDefinition ad = new AgentConfigDTO.AgentDefinition();
                    ad.setName(a.getName());
                    ad.setInstruction(a.getInstruction());
                    ad.setDescription(a.getDescription());
                    ad.setOutputKey(a.getOutputKey());
                    return ad;
                }).collect(Collectors.toList()));
            }
            if (module.getRunner() != null) {
                dto.setRunnerAgentName(module.getRunner().getAgentName());
                dto.setPluginNameList(module.getRunner().getPluginNameList());
            }
        }
        return dto;
    }
}
```

---

## 第三步：后端 — 新增 Controller

新建 `ai-agent-scaffold-trigger/src/main/java/org/example/trigger/http/AgentConfigController.java`：

```java
package org.example.trigger.http;

import lombok.extern.slf4j.Slf4j;
import org.example.api.dto.AgentConfigDTO;
import org.example.api.response.Response;
import org.example.domain.agent.service.config.IAgentConfigService;
import org.example.types.enums.ResponseCode;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-config")
@CrossOrigin(origins = "*")
public class AgentConfigController {

    @Resource
    private IAgentConfigService agentConfigService;

    /**
     * 保存/更新智能体配置
     */
    @PostMapping("/save")
    public Response<String> saveAgentConfig(@RequestBody AgentConfigDTO config) {
        try {
            String filename = agentConfigService.saveAgentConfig(config);
            return Response.<String>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("智能体配置保存成功，已热装配")
                    .data(filename)
                    .build();
        } catch (Exception e) {
            log.error("保存智能体配置失败", e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("保存失败: " + e.getMessage())
                    .build();
        }
    }

    /**
     * 获取所有已配置智能体列表
     */
    @GetMapping("/list")
    public Response<List<AgentConfigDTO>> listAgentConfigs() {
        try {
            List<AgentConfigDTO> list = agentConfigService.listAgentConfigs();
            return Response.<List<AgentConfigDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(list)
                    .build();
        } catch (Exception e) {
            log.error("查询配置列表失败", e);
            return Response.<List<AgentConfigDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("查询失败")
                    .build();
        }
    }

    /**
     * 删除智能体配置
     */
    @DeleteMapping("/delete/{appName}")
    public Response<Void> deleteAgentConfig(@PathVariable String appName) {
        try {
            agentConfigService.deleteAgentConfig(appName);
            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("配置已删除，重启后生效")
                    .build();
        } catch (Exception e) {
            log.error("删除配置失败", e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("删除失败")
                    .build();
        }
    }
}
```

### POM 依赖补充

在 `ai-agent-scaffold-domain/pom.xml` 中加入 Jackson YAML：

```xml
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
```

---

## 第四步：新增 API 接口定义

在 `ai-agent-scaffold-api/src/main/java/org/example/api/IAgentService.java` 或者单独的文件中：

```java
package org.example.api;

import org.example.api.dto.AgentConfigDTO;
import org.example.api.response.Response;
import java.util.List;

public interface IAgentConfigApi {
    Response<String> saveAgentConfig(AgentConfigDTO config);
    Response<List<AgentConfigDTO>> listAgentConfigs();
    Response<Void> deleteAgentConfig(String appName);
}
```

---

## 第五步：前端 — 智能体配置页面

在之前设计的 `frontend-prompt.md` 的基础上，新增一个配置页面。

### 路由设计

```typescript
// router.ts 新增路由
{
  path: '/designer',
  name: 'AgentDesigner',
  component: () => import('@/views/AgentDesigner.vue')
}
```

### 页面结构

```
┌─────────────────────────────────────────────────────────┐
│  >_ AGENT OS                          [返回 Dashboard]   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─── 基础信息 ──────────────────────────────────────┐   │
│  │  App Name    [_______________]  Agent ID  [_______] │   │
│  │  Agent Name  [_______________]  Desc     [_______] │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─── LLM 配置 ────────────────────────────────────────┐ │
│  │  Base URL   [https://api.openai.com_____________]    │ │
│  │  API Key    [*******************************]  👁️   │ │
│  │  Model      [gpt-4.1                   ▼]           │ │
│  └─────────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─── 智能体定义 ──────────────────────────────────────┐ │
│  │  ┌─ Agent 1 ──────────────────────────────────────┐ │ │
│  │  │ Name: [onlyAgent]   Desc: [________]           │ │ │
│  │  │ Instruction:                                    │ │ │
│  │  │ ┌───────────────────────────────────────────┐   │ │ │
│  │  │ │ 通过百度检索...                           │   │ │ │
│  │  │ │                                           │   │ │ │
│  │  │ └───────────────────────────────────────────┘   │ │ │
│  │  └─────────────────────────────────────────────────┘ │ │
│  │  [+ Add Agent]                                       │ │
│  └─────────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─── MCP 工具（可选） ────────────────────────────────┐ │
│  │  [SSE ▼] Name [baidu-search]                       │ │
│  │  URI    [https://appbuilder.baidu.com/...]          │ │
│  │  Endpoint [sse?api_key=...]                        │ │
│  │  Timeout [5000]ms                    [× 删除]      │ │
│  │  [+ Add MCP Tool]                                   │ │
│  └─────────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─── 工作流（可选） ──────────────────────────────────┐ │
│  │  Type [Loop ▼]  Name [review-loop]                 │ │
│  │  Sub-agents [onlyAgent, criticAgent...]            │ │
│  │  Max Iterations [3]               [× 删除]         │ │
│  │  [+ Add Workflow]                                   │ │
│  └─────────────────────────────────────────────────────┘   │
│                                                          │
│  ┌─── Runner ──────────────────────────────────────────┐ │
│  │  Entry Agent [onlyAgent          ▼]                 │ │
│  │  Plugins     [myTestPlugin, myLogPlugin]             │ │
│  └─────────────────────────────────────────────────────┘   │
│                                                          │
│  [✕ 取消]                             [🚀 部署智能体]    │
└─────────────────────────────────────────────────────────┘
```

### TypeScript 类型

```typescript
// 前端配置表单类型（与后端 AgentConfigDTO 对应）
interface AgentConfigForm {
  appName: string;
  agentId: string;
  agentName: string;
  agentDesc: string;
  baseUrl: string;
  apiKey: string;
  completionsPath: string;
  embeddingsPath: string;
  model: string;
  toolMcpList: McpToolConfig[];
  agents: AgentDefinition[];
  agentWorkflows: WorkflowConfig[];
  runnerAgentName: string;
  pluginNameList: string[];
}

interface McpToolConfig {
  type: 'sse' | 'stdio' | 'local';
  name: string;
  baseUri?: string;
  sseEndpoint?: string;
  requestTimeout?: number;
  command?: string;
  args?: string[];
  env?: Record<string, string>;
}

interface AgentDefinition {
  name: string;
  instruction: string;
  description: string;
  outputKey?: string;
}

interface WorkflowConfig {
  type: 'loop' | 'parallel' | 'sequential';
  name: string;
  subAgents: string[];
  description: string;
  maxIterations: number;
}
```

### API 调用方法

```typescript
// api/agentConfig.ts
import axios from 'axios';
import type { AgentConfigForm, ApiResponse } from './types';

const API_BASE = 'http://localhost:8091/api/v1';

/** 保存智能体配置 */
export function saveAgentConfig(config: AgentConfigForm) {
  return axios.post<ApiResponse<string>>(`${API_BASE}/agent-config/save`, config);
}

/** 获取已配置智能体列表 */
export function listAgentConfigs() {
  return axios.get<ApiResponse<AgentConfigForm[]>>(`${API_BASE}/agent-config/list`);
}

/** 删除配置 */
export function deleteAgentConfig(appName: string) {
  return axios.delete<ApiResponse<null>>(`${API_BASE}/agent-config/delete/${appName}`);
}
```

---

## 补充：让项目在启动时也扫描外部目录

如果想要项目启动时也自动加载前端已保存的 YAML 配置（而非仅依赖 `spring.config.import`），可以在 `AiAgentAutoConfig` 中增加扫描 `data/agent-configs/` 目录的逻辑：

```java
// 在 AiAgentAutoConfig.init() 方法开头补充：
@PostConstruct
public void init() {
    // 1. 先从外部目录加载额外配置
    loadExternalConfigs();

    // 2. 原有装配逻辑
    if (aiAgentAutoConfigProperties.getTables() == null || ...) { ... }
    armoryService.acceptArmoryAgents(...);
}

private void loadExternalConfigs() {
    try {
        Path extDir = Paths.get("data", "agent-configs");
        if (Files.exists(extDir)) {
            Files.list(extDir)
                .filter(p -> p.toString().endsWith(".yml"))
                .forEach(this::loadYamlFile);
        }
    } catch (IOException e) {
        log.warn("加载外部配置文件失败", e);
    }
}
```

但更简单的方式是把外部目录也加到 `spring.config.import` 中（需要支持通配符），或者用 YAML 的 `include` 机制。对于演示阶段，直接在 `application-dev.yml` 中 import 外部路径即可：

```yaml
spring:
  config:
    import:
      - classpath:agent/only-one-agent.yml
      - file:data/agent-configs/*.yml    # 追加扫描外部目录
```

---

## 补充说明

| 问题 | 说明 |
|------|------|
| **API Key 安全性** | 目前 apiKey 明文传输和存储。生产环境应使用 HTTPS + 后端加密存储 |
| **热装配可靠性** | 热装配通过 `ArmoryService.acceptArmoryAgents()` 动态注册 Spring Bean，AgentWorkflow 节点销毁需手动处理。简单场景够用，生产建议用 `@RefreshScope` |
| **重复配置** | 如果保存同名 agent，会覆盖旧配置。热装配会注册第二个同名 Bean 导致冲突，建议保存时先检查 |
| **适用场景** | 开发阶段 / 内部工具 / Demo 演示非常合适。生产环境建议将配置存数据库 |
