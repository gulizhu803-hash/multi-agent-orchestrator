# 修改日志

## 2026-05-26

### 1. 敏感信息统一至 env-config.properties

将所有硬编码的 API key、密码等敏感信息集中到 `env-config.properties`，通过 `spring.config.import` 加载。

涉及文件：
- `env-config.properties`（新建）
- `application.yml` — 添加 `spring.config.import: optional:file:./env-config.properties`
- `.gitignore` — 排除 `env-config.properties`

### 2. Agent YAML 变量化

所有 agent 配置文件的 `base-url` 和 `model` 替换为占位变量，支持通过 `env-config.properties` 一键切换 LLM。

| 文件 | 变更内容 |
|------|----------|
| `test_agent.yml` | `base-url: ${AI_BASE_URL}`, `model: ${AI_MODEL}` |
| `only-one-agent.yml` | 同上 |
| `complaint-agent.yml` | 同上 |
| `tech-blog-writer.yml` | 同上 |
| `demo-agent.yml` | 同上（4 个 agent 配置全部替换） |

切换模型只需修改 `env-config.properties`：
```properties
AI_BASE_URL=https://api.deepseek.com
AI_MODEL=deepseek-chat
AI_AGENT_OPENAI_API_KEY=sk-xxx
```

### 3. 测试基础设施修复

- 新建 `src/test/resources/agent/test-agent-test.yml` — 不含 SSE MCP 的测试专用 agent 配置（包含 agent 100003 和 300001）
- `src/test/resources/application-test.yml` — 导入测试 agent 配置，排除 DataSource 自动配置
- `TechBlogWriterTest.java` — 移除显式 API key 属性覆盖
- `src/main/resources/application-test.yml` — 恢复原状（测试层有独立的覆盖文件）

### 4. [待实现] Per-Agent LLM 模型配置（方案二）

**需求：** 同一工作流内，不同 agent 角色使用不同的 LLM 模型。

**设计：**

- `AiAgentConfigTableVO.Module.Agent` 增加两个可选字段：
  - `String model` — 覆写该 agent 使用的模型名
  - `AiApi aiApi` — 覆写该 agent 的 API 配置（base-url / api-key）

- `DefaultArmoryFactory.DynamicContext` 增加 `List<ToolCallback> toolCallbackList` 字段，由 `ChatModelNode` 写入，供 `AgentNode` 复用。

- `AgentNode.doApply()` 中遍历 agent 时判断：如果 agent 指定了 `model` 或 `aiApi`，则创建独立的 `OpenAiApi` + `OpenAiChatModel`；否则沿用 table 级别的共享 `ChatModel`。

**涉及文件：**
- `ai-agent-scaffold-domain/.../AiAgentConfigTableVO.java`
- `ai-agent-scaffold-domain/.../DefaultArmoryFactory.java`
- `ai-agent-scaffold-domain/.../ChatModelNode.java`
- `ai-agent-scaffold-domain/.../AgentNode.java`

**配置示例：**
```yaml
agents:
  - name: writer
    # 使用默认模型 gpt-4.1
    instruction: 你是一名技术博客作者...
  - name: reviewer
    model: deepseek-chat          # 覆写模型
    ai-api:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_KEY}
    instruction: 你是一名代码审查专家...
```

---

### 5. SpringAiApiTest 修复

`SpringAiApiTest` 原来直接使用 `System.getenv()` 读取 API 配置，而 `env-config.properties` 加载到 Spring 环境后并不会进入系统环境变量，导致该测试在未设环境变量时获取不到 API key。

**修复：** 改用 `FileReader` 直接加载 `env-config.properties`，并保留 `System.getenv()` 回退。

涉及文件：
- `src/test/java/org/example/test/api/model/SpringAiApiTest.java`

### 6. 跨模块 bean 冲突修复

`ChatServiceTest` / `TechBlogWriterTest` / `AiAgentAutoConfigTest` 因 `.m2` 本地仓库中 `ai-agent-scaffold-domain` / `ai-agent-scaffold-trigger` 的旧 JAR 残留已删除的 `AgentConfigService` 类，导致 Spring context 刷新时报 `ConflictingBeanDefinitionException`。

**修复：** 从项目根目录执行 `mvn install -DskipTests` 全量构建，刷新所有模块的 JAR 后测试通过。

### 7. 新增环境变量 AI_COMPLETIONS_PATH

Qwen（通义千问）的 API URL 结构特殊（`https://dashscope.aliyuncs.com/compatible-mode/v1/`），`DefaultUriBuilderFactory` 对绝对路径 URI（以 `/` 开头）会替换 base URL 的 path 部分，导致 `compatible-mode` 段丢失。

**修复：** 新增 `AI_COMPLETIONS_PATH` 环境变量，与 `AI_BASE_URL` 分离，支持相对路径（`chat/completions`）或绝对路径（`v1/chat/completions`）按需配置。

### 8. SSE MCP API Key 占位符未解析修复

`tech-blog-writer.yml` 中 `sse-endpoint: sse?api_key=${AI_AGENT_MCP_API_KEY}` 的 `${AI_AGENT_MCP_API_KEY}` 占位符在 `@ConfigurationProperties` 绑定中未解析（Spring Boot 对深层嵌套的 `Map<String, List<POJO>>` 结构不执行占位符解析），导致字面量字符串 `sse?api_key=${AI_AGENT_MCP_API_KEY}` 原样传递给 MCP SDK，百度 MCP 认证失败。

**根因分析：**
- MCP SDK 0.14.0 的 `connect()` 方法使用 `Utils.resolveUri(baseUri, sseEndpoint)`，内部调用 `URI.resolve()` — 能正确处理相对 URI 中的 query params（`sse?api_key=xxx` → `.../mcp/sse?api_key=xxx`）。URL 构建本身没有 bug。
- 唯一问题是 `${AI_AGENT_MCP_API_KEY}` 未被 Spring 解析。

**修复：** 在 `SSEToolMcpCreateService` 中注入 Spring `Environment`，对 `baseUri` 和 `sseEndpoint` 显式调用 `environment.resolvePlaceholders()` 解析 `${...}` 占位符。这样无论值来自环境变量、`.env` 还是 `@PropertySource`，都能正确解析。

涉及文件：
- `SSEToolMcpCreateService.java`（domain 层）
- 同步更新了 `tech-blog-agent` 项目和 Maven archetype 模板
