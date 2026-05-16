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
