# ai-agent-scaffold

AI Agent 项目脚手架，基于 DDD 分层架构，通过 Maven Archetype 一键生成新项目。

## 项目简介

这是一个 Maven Archetype 项目，用于快速生成 AI Agent 应用的基础工程结构。

### 模块结构

```
├── ai-agent-scaffold-api            # API 层 — 对外接口定义
├── ai-agent-scaffold-app             # 应用层 — 启动入口、配置
├── ai-agent-scaffold-domain          # 领域层 — 核心业务逻辑
├── ai-agent-scaffold-trigger         # 触发层 — HTTP/消息处理
├── ai-agent-scaffold-infrastructure  # 基础设施层 — 数据持久化等
├── ai-agent-scaffold-types           # 类型定义 — 通用枚举、DTO
├── docs/dev-ops                      # 部署脚本
└── deploy-scaffold                   # Archetype 部署仓库
```

使用该脚手架生成的新项目会自动替换包名、模块名和应用名称，无需手动修改。

## 构建与使用

### 构建 Archetype

在项目根目录执行：

**Linux / macOS / Git Bash：**

```bash
./build-archetype.sh
```

**Windows（cmd）：**

```cmd
build-archetype.bat
```

构建产物：
- 生成 Archetype JAR 到 `target/archetype-nginx-repo/`，可直接部署到 Nginx 作为内部 Maven 仓库
- 脚本会自动清理无用文件、替换硬编码名称为模板变量、修复非 XML 文件的变量替换配置

### 生成新项目

安装 Archetype 到本地仓库后即可生成项目：

```bash
# 安装到本地 Maven 仓库
mvn install:install-file \
  -Dfile=target/ai-agent-scaffold-lite-archetype/target/ai-agent-scaffold-archetype-1.0.jar \
  -DpomFile=target/ai-agent-scaffold-lite-archetype/pom.xml

# 生成新项目
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

生成的 `your-project` 会包含完整的 DDD 模块结构、Docker 部署配置及启动脚本。

