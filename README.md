# multi-agent-orchestrator

**AI Agent 引擎 — 可配置多 Agent 编排系统**

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-latest-brightgreen)](https://spring.io/projects/spring-ai)
[![Maven Archetype](https://img.shields.io/badge/Maven-Archetype-orange)](https://maven.apache.org/archetype/)

一个基于 **Spring AI + Google ADK** 的 AI Agent 引擎，支持 YAML 声明式配置多 Agent 编排，通过 Maven Archetype 一键生成新项目。

---

## 特性

- **声明式 Agent 配置** — 通过 YAML 定义 Agent 行为、模型、工具，无需编写编排代码
- **多 Agent 编排** — 支持 Sequential（串行）、Parallel（并行）、Loop 等工作流模式
- **MCP 协议集成** — 内置 SSE MCP 客户端，接入百度搜索等外部 MCP Server
- **多模型切换** — 一行配置切换 OpenAI / DeepSeek / Qwen 等模型
- **DDD 分层架构** — API → App → Domain → Trigger → Infrastructure，清晰分离关注点
- **Maven Archetype 脚手架** — 一条命令生成完整项目骨架
- **Docker 部署** — 自带 Dockerfile 和 docker-compose 编排（MySQL + Redis）

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+（可选，dev profile 需要）
- Redis（可选）

### 1. 配置 API Key

```bash
cp .env.example ai-agent-scaffold-app/env-config.properties
```

编辑 `ai-agent-scaffold-app/env-config.properties`，填入你的 API Key：

```properties
# 模型 API Key（OpenAI / DeepSeek / Qwen 三选一）
AI_AGENT_OPENAI_API_KEY=sk-your-api-key-here

# 百度 MCP 搜索服务 Key（可选）
AI_AGENT_MCP_API_KEY=bce-v3/your-mcp-api-key-here

# 数据库密码
MYSQL_ROOT_PASSWORD=your-db-password
```

> ⚠️ `env-config.properties` 已被 `.gitignore` 排除，不会提交到仓库。

### 2. 启动应用

```bash
cd ai-agent-scaffold-app
mvn spring-boot:run -Dspring.profiles.active=dev
```

### 3. 生成新项目（Archetype）

```bash
# 构建 Archetype
./build-archetype.sh      # Linux / macOS
build-archetype.bat       # Windows

# 用 Archetype 生成新项目
mvn archetype:generate \
  -DarchetypeGroupId=org.example \
  -DarchetypeArtifactId=ai-agent-scaffold-archetype \
  -DarchetypeVersion=1.0 \
  -DgroupId=com.yourcompany \
  -DartifactId=your-project \
  -Dversion=1.0-SNAPSHOT \
  -Dpackage=com.yourcompany.yourproject \
  -DarchetypeCatalog=local \
  -DinteractiveMode=false
```

## 项目结构

```
├── ai-agent-scaffold-api            # API 层 — 对外接口定义（DTO、RPC 接口）
├── ai-agent-scaffold-app             # 应用层 — 启动入口、YAML 配置、env 配置
│   └── src/main/resources/agent/    # Agent 定义（YAML）
│       ├── demo-agent.yml           #   示例：多 Agent 编排
│       ├── complaint-agent.yml      #   示例：投诉处理 Agent
│       └── tech-blog-writer.yml     #   示例：技术博客生成
├── ai-agent-scaffold-domain          # 领域层 — 核心业务逻辑
│   └── agent/service/armory/        #   Agent 运行时引擎
│       ├── matter/                  #     模型适配、MCP 客户端、工具注册
│       └── tree/                    #     策略路由（责任链模式）
├── ai-agent-scaffold-trigger         # 触发层 — HTTP Controller、消息监听
├── ai-agent-scaffold-infrastructure  # 基础设施层 — Repository 实现、数据持久化
├── ai-agent-scaffold-types           # 类型定义 — 通用枚举、常量
├── docs/dev-ops                      # DevOps — Docker Compose、Nginx 配置
└── deploy-scaffold                   # Archetype 部署文件
```

## Agent 配置示例

```yaml
ai:
  agent:
    config:
      tables:
        myAgent:
          app-name: myAgent
          agent:
            agent-id: 100001
            agent-name: 我的智能体
            agent-desc: 一个示例智能体
          module:
            ai-api:
              base-url: ${AI_BASE_URL}
              api-key: ${AI_AGENT_OPENAI_API_KEY}
              completions-path: ${AI_COMPLETIONS_PATH}
            chat-model:
              model: ${AI_MODEL}
              tool-mcp-list:
                - sse:
                    name: baidu-search
                    base-uri: https://appbuilder.baidu.com/v2/ai_search/mcp/
                    sse-endpoint: sse?api_key=${AI_AGENT_MCP_API_KEY}
                    request-timeout: 30000
            agents:
              - name: myAgent
                description: 执行任务
                instruction: |
                  你是一个专业的 AI 助手。
            runner:
              agent-name: myAgent
```

## 模型切换

在 `env-config.properties` 中切换模型只需修改 3 行：

| 模型 | BASE_URL | COMPLETIONS_PATH | MODEL |
|------|----------|-----------------|-------|
| OpenAI | `https://api.openai.com` | `v1/chat/completions` | `gpt-4.1` |
| DeepSeek | `https://api.deepseek.com` | `v1/chat/completions` | `deepseek-chat` |
| Qwen (通义千问) | `https://dashscope.aliyuncs.com/compatible-mode/v1/` | `chat/completions` | `qwen-plus` |

## 技术栈

- **AI 框架**：Spring AI + Google Agent Development Kit (ADK)
- **后端**：Spring Boot 3.x, MyBatis, MySQL
- **MCP 协议**：SSE Transport, Tool Callbacks
- **构建工具**：Maven Archetype, Docker
- **测试**：JUnit 5, Spring Boot Test

## 许可证

[Apache License 2.0](LICENSE)
