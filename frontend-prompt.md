# AI Agent 编排平台 · 前端开发设计规范

## 项目定位

你正在构建一个 **AI 智能体编排平台的管理控制台**。这不是普通的聊天 UI，而是一个面向开发者/运营者的**高级工具型产品**。设计基调：沉稳、精密、科技感，如同专业 IDE 与 AI 助手的结合体。

后端：Spring Boot 3.4 + Google ADK + Spring AI，运行于 `localhost:8091`，已配置 `@CrossOrigin(origins = "*")`。

---

## 🎨 视觉设计语言（强制执行）

### 美学方向：Deep Space Terminal（深空终端）

- **整体基调**：深色主题为唯一主题，禁止浅色背景
- **配色系统**：
    - 背景层：`#080B14`（最深）→ `#0D1117`（主背景）→ `#111827`（卡片层）
    - 强调色：`#00D4FF`（冷青蓝，主交互色）
    - 辅助色：`#7C3AED`（紫罗兰，点缀用）
    - 渐变：从 `#00D4FF` 到 `#7C3AED` 的对角渐变，用于高亮元素和边框
    - 文字：`#F1F5F9`（主文字）/ `#64748B`（次文字）/ `#94A3B8`（辅助说明）
    - 危险/错误：`#EF4444`；成功：`#10B981`

- **排版**：
    - 展示级标题：`'Space Mono'`（Google Fonts）—— 等宽科技感
    - 正文：`'DM Sans'`（Google Fonts）—— 现代可读
    - 代码块：`'JetBrains Mono'`（Google Fonts）—— 专业代码字体
    - 字号层级严格：12 / 13 / 14 / 16 / 18 / 24 / 32 / 48px

- **质感细节**：
    - 所有卡片：glassmorphism 风格，`backdrop-filter: blur(20px)`，半透明边框 `rgba(255,255,255,0.06)`
    - 发光效果：激活状态元素有 `box-shadow: 0 0 20px rgba(0, 212, 255, 0.3)`
    - 背景纹理：主背景加 SVG 噪点纹理（opacity 0.03）+ 极细网格线（opacity 0.04）
    - 渐变边框：重要卡片使用 `border: 1px solid transparent; background-clip: padding-box` 配合伪元素实现渐变描边
    - 扫描线动画：页面顶部有细线扫描的 subtle 动效

### CSS 变量（必须定义）

```css
:root {
  /* 背景层 */
  --bg-void: #080B14;
  --bg-base: #0D1117;
  --bg-surface: #111827;
  --bg-elevated: #1A2235;
  --bg-hover: #1E2A3A;

  /* 强调色 */
  --accent-primary: #00D4FF;
  --accent-secondary: #7C3AED;
  --accent-gradient: linear-gradient(135deg, #00D4FF 0%, #7C3AED 100%);

  /* 文字 */
  --text-primary: #F1F5F9;
  --text-secondary: #94A3B8;
  --text-muted: #64748B;
  --text-accent: #00D4FF;

  /* 边框 */
  --border-subtle: rgba(255, 255, 255, 0.06);
  --border-default: rgba(255, 255, 255, 0.1);
  --border-accent: rgba(0, 212, 255, 0.3);

  /* 阴影 */
  --glow-primary: 0 0 20px rgba(0, 212, 255, 0.25);
  --glow-strong: 0 0 40px rgba(0, 212, 255, 0.4);
  --shadow-card: 0 4px 24px rgba(0, 0, 0, 0.4);

  /* 圆角 */
  --radius-sm: 6px;
  --radius-md: 12px;
  --radius-lg: 16px;
  --radius-xl: 24px;

  /* 动画 */
  --ease-out-expo: cubic-bezier(0.19, 1, 0.22, 1);
  --ease-in-out-quart: cubic-bezier(0.76, 0, 0.24, 1);
}
```

---

## 🏗️ 技术栈

- **框架**: Vue 3 + Vite + TypeScript（Composition API，`<script setup>` 语法）
- **UI 组件库**: Element Plus（按需引入，全部覆盖为自定义暗色主题，禁止使用默认样式）
- **状态管理**: Pinia
- **路由**: Vue Router 4
- **HTTP**: Axios（封装统一拦截器）
- **Markdown**: `markdown-it` + `highlight.js`（代码块用 `github-dark` 主题）
- **动画**: 使用纯 CSS transitions/animations，关键动效用 `@vueuse/motion`
- **字体引入**: Google Fonts（Space Mono + DM Sans + JetBrains Mono）
- **图标**: `lucide-vue-next`（线条图标，符合精密科技风格）

---

## 📐 页面设计规范

### 全局布局

```
┌─────────────────────────────────────────┐
│  顶部导航栏 (56px，带渐变底部分割线)       │
├──────────┬──────────────────────────────┤
│          │                              │
│  左侧边栏 │      主内容区                │
│  (260px) │                              │
│          │                              │
└──────────┴──────────────────────────────┘
```

**顶部导航栏**：
- 背景：`var(--bg-void)` + `backdrop-filter: blur(12px)`
- 左：Logo（Space Mono 字体，`>_ AGENT OS` 样式，带光标闪烁动画）
- 右：用户 ID 显示（可点击修改）+ 连接状态指示灯（绿点脉冲动画）
- 底部：1px 渐变分割线（从 `#00D4FF` 到 `#7C3AED`，opacity 0.4）

**左侧边栏**：
- 背景：`var(--bg-surface)`，右侧 `border-right: 1px solid var(--border-subtle)`
- 顶部：「AGENTS」标签（全大写，letter-spacing: 0.15em，text-muted 色）
- 内容：智能体列表（每项高 52px，hover 时左侧出现 2px 发光线条）
- 激活项：`var(--bg-elevated)` 背景 + 左侧 `var(--accent-primary)` 3px 竖线 + 文字高亮

---

### 页面 1：智能体列表（首页 Dashboard）

**设计理念**：指挥中心 Dashboard，展示可用「武器库」

**布局**：
- 顶部英雄区（Hero）：
    - 大标题 `AGENT NETWORK`（48px，Space Mono，带渐变色）
    - 副标题（灰色，14px）
    - 右侧：实时数据指标（智能体数量、在线状态，数字用 `#00D4FF` 高亮）

- 智能体卡片网格（3列，gap: 20px）：
    - 卡片尺寸：自适应宽度，高度 180px
    - 背景：`var(--bg-surface)`，glassmorphism
    - 左上角：智能体类型图标（线性图标，40px，带渐变背景圆形）
    - 右上角：状态徽章（`ACTIVE` / `OFFLINE`，极小字体，带对应色点）
    - 中部：Agent 名称（18px，Space Mono），描述（13px，--text-secondary）
    - 底部分割线 + 操作区：「LAUNCH SESSION」按钮（幽灵按钮风格，hover 时填充渐变）
    - 卡片 hover：`transform: translateY(-4px)` + 顶部渐变边框出现 + 阴影增强
    - 卡片入场：staggered fade-in-up（每张延迟 80ms）

**用户 ID 处理**：
- 右上角浮动小组件，显示「USER: usr_xxxxxxxx」
- 首次访问弹出精美 Modal 让用户确认/修改（不用丑陋的 prompt()）

---

### 页面 2：聊天对话页

**设计理念**：高级 AI 工作台，不是微信聊天，是专业工具

**三栏布局（聊天页专属）**：
```
┌──────────┬─────────────────────────┬─────────────┐
│  Agent   │     消息流              │   侧边信息  │
│  信息栏  │                         │   面板      │
│  (240px) │                         │   (200px)   │
└──────────┴─────────────────────────┴─────────────┘
```

**左侧 Agent 信息栏**：
- Agent 名称（Space Mono，大字）
- Agent 描述（小字，可展开）
- Session ID（等宽字体，可复制，显示前 20 字符 + `...`）
- 消息数量统计
- 「NEW SESSION」重置按钮

**消息流区域（中间主区）**：

消息气泡设计：
- **用户消息**：右对齐，`var(--bg-elevated)` 背景，`border: 1px solid var(--border-default)`，右侧无头像，简洁
- **AI 消息**：左对齐，`var(--bg-surface)` 背景，左边缘 `border-left: 2px solid var(--accent-primary)` 竖线，顶部显示 Agent 名称（极小 badge）
- **工具调用消息**（MCP 工具）：特殊样式，`var(--bg-void)` 背景，`border: 1px solid rgba(124, 58, 237, 0.3)`，左侧紫色线条，顶部显示「⚡ TOOL CALL: 工具名」（等宽字体）
- **消息时间**：气泡下方极小灰字，hover 时才完整显示

流式打字效果：
- AI 回复时，末尾显示闪烁光标 `▋`（纯 CSS animation）
- 不要逐字闪烁，要平滑追加文字，光标始终在最末位置

Markdown 渲染规范：
- 代码块：`github-dark` 主题，有复制按钮（右上角），带语言标签
- 行内代码：`#00D4FF` 色，`var(--bg-void)` 背景
- 标题：颜色比正文亮，保持层级感
- 列表：`::before` 用 `▸` 替代默认圆点
- 表格：极细边框，偶数行略微提亮背景

**底部输入区**：
- 与消息流之间有 `backdrop-filter: blur` 的半透明渐变分隔
- 输入框：多行 textarea，无边框，`var(--bg-elevated)` 背景，整体圆角容器有渐变描边（focus 时描边亮度提升）
- 左下角：模式切换（`STREAM` / `SYNC` toggle，胶囊样式，颜色区分）
- 右下角：发送按钮（方形，渐变背景，内有 `↑` 图标，loading 时变为旋转圆环）
- 提示文字：`placeholder` 用 `Shift + Enter 换行 · Enter 发送` 替代默认文字

**右侧信息面板（可收起）**：
- 当前对话统计（token 数估算展示）
- 响应时间记录（最近 5 次，小图表）
- MCP 工具调用历史（紧凑列表）

**状态设计**：
- **空状态**：居中大图，ASCII art 风格的机器人图形 + `READY FOR INPUT` 文字 + 2-3 个建议问题按钮（点击自动填充）
- **Loading 状态**：三点 `···` 动画（每个点依次放大，间隔 200ms）+ 文字「Agent is thinking...」
- **错误状态**：底部 toast 弹出（`#EF4444` 色，带错误码显示），保留输入内容
- **首次加载慢提示**：Loading 时显示「Initializing LLM... This may take 3-10s」

---
### 页面 3：智能体设计器 Agent Designer（新增）

**设计理念**：智能体配置的控制台，像 Vercel 的部署配置页一样精密，分区块编排

**布局**：顶部返回按钮 + 页面标题，下方单列滚动表单，按功能分组为折叠卡片

**页面结构**：
```
┌──────────────────────────────────────────────────┐
│  ← 返回 Dashboard        AGENT DESIGNER          │
├──────────────────────────────────────────────────┤
│                                                   │
│  ┌─── 基础信息 ───────────────────────────────┐   │
│  │  App Name   [testAgent03          ] 必填    │   │
│  │  Agent ID   [100003               ] 必填    │   │
│  │  Agent Name [我的智能体            ] 必填    │   │
│  │  Agent Desc [智能体描述            ]        │   │
│  └──────────────────────────────────────────────┘   │
│                                                   │
│  ┌─── LLM API 配置 ────────────────────────────┐  │
│  │  Base URL  [https://api.openai.com     ]     │  │
│  │  API Key   [********************] 👁️ 显示/隐藏│  │
│  │  Model     [gpt-4.1               ▼] 下拉    │  │
│  │  Completions Path [v1/chat/completions]      │  │
│  │  Embeddings Path  [v1/embeddings     ]       │  │
│  └──────────────────────────────────────────────┘  │
│                                                   │
│  ┌─── Agents 定义 ─────────────────────────────┐  │
│  │  ┌─ Agent 1 ──────────────────────────────┐ │  │
│  │  │ Name: [onlyAgent]  OutputKey: [___]    │ │  │
│  │  │ Description: [小傅哥学习项目计划]       │ │  │
│  │  │ Instruction: ┌──────────────────┐      │ │  │
│  │  │              │ 通过百度检索...  │      │ │  │
│  │  │              └──────────────────┘      │ │  │
│  │  │  [× 删除]                             │ │  │
│  │  └────────────────────────────────────────┘ │  │
│  │  [+ Add Agent]                              │  │
│  └──────────────────────────────────────────────┘  │
│                                                   │
│  ┌─── MCP 工具（可选） ────────────────────────┐  │
│  │  Type: [SSE ▼]  Name: [baidu-search]       │  │
│  │  Base URI: [https://...]                    │  │
│  │  SSE Endpoint: [sse?api_key=...]            │  │
│  │  Request Timeout: [30000] ms               │  │
│  │  [× 删除]  [+ Add MCP Tool]                │  │
│  └──────────────────────────────────────────────┘  │
│                                                   │
│  ┌─── 工作流（可选） ──────────────────────────┐  │
│  │  Type: [loop ▼]  Name: [review-loop]       │  │
│  │  Sub-agents: [agent1, agent2, ...]         │  │
│  │  Max Iterations: [3]                       │  │
│  │  [× 删除]  [+ Add Workflow]                │  │
│  └──────────────────────────────────────────────┘  │
│                                                   │
│  ┌─── Runner ──────────────────────────────────┐  │
│  │  Entry Agent: [onlyAgent ▼] 下拉选择已有agent│  │
│  │  Plugins: [myPlugin] 标签输入                │  │
│  └──────────────────────────────────────────────┘  │
│                                                   │
│  ┌──────────────────────────────────────────────┐  │
│  │  [取消]              [🚀 保存并部署]         │  │
│  └──────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

**交互设计**：
- 每个配置区块为独立卡片，可折叠（标题栏点击切换）
- 底部悬浮操作栏（滚动时固定），显示「保存并部署」主按钮 +「取消」幽灵按钮
- API Key 字段默认遮罩，点击 👁️ 切换可见
- 下拉选择（Model、Agent Type）使用 Element Plus Select，全局暗色覆盖
- 标签输入（Plugins）使用 Element Plus Tag Input 风格
- Agent Instruction 为多行文本框（min-height: 120px），等宽字体

**状态设计**：
- **空状态**：首次进入，所有字段空，placeholder 提示示例值
- **编辑状态**：从 /designer/:appName 进入，回填已有配置数据
- **保存中**：按钮 loading +「正在部署...」文字
- **保存成功**：按钮变为绿色 ✅「部署成功」+ 自动 2 秒后跳转回 Dashboard
- **保存失败**：按钮变为红色 ❌ 显示错误信息 + 保留表单数据
- **删除确认**：点击删除弹确认对话框，暗色 Modal，红色强调

### 后端接口

所有接口基路径：`http://localhost:8091/api/v1/`

统一响应格式：
```json
{ "code": "0000", "info": "成功", "data": { ... } }
```

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取 Agent 列表 | GET | `/query_ai_agent_config_list` | 无参数 |
| 创建会话 | GET | `/create_session?userId=&agentId=` | 返回 sessionId |
| 普通聊天 | POST | `/chat` | JSON body |
| 流式聊天 | POST | `/chat_stream` | 返回 ReadableStream |
| 保存智能体配置 | POST | `/agent-config/save` | JSON body，返回 filename |
| 智能体配置列表 | GET | `/agent-config/list` | 获取所有已配置智能体 |
| 删除智能体配置 | DELETE | `/agent-config/delete/{appName}` | 按 appName 删除 |

#### 智能体配置接口详情

**POST `/agent-config/save`**

请求体（完整结构）：
```json
{
  "appName": "myAgent",
  "agentId": "100001",
  "agentName": "我的智能体",
  "agentDesc": "智能体描述",
  "baseUrl": "https://api.openai.com",
  "apiKey": "sk-xxx",
  "completionsPath": "v1/chat/completions",
  "embeddingsPath": "v1/embeddings",
  "model": "gpt-4.1",
  "toolMcpList": [
    {
      "type": "sse",
      "name": "baidu-search",
      "baseUri": "https://example.com/mcp",
      "sseEndpoint": "sse",
      "requestTimeout": 30000,
      "command": "",
      "args": [],
      "env": null
    }
  ],
  "agents": [
    {
      "name": "myAgent",
      "instruction": "你是一个有用的助手",
      "description": "主智能体",
      "outputKey": ""
    }
  ],
  "agentWorkflows": [],
  "runnerAgentName": "myAgent",
  "pluginNameList": ["myPlugin"]
}
```

响应：
```json
{ "code": "0000", "info": "智能体配置保存成功，已热装配", "data": "myAgent.yml" }
```

**GET `/agent-config/list`**

响应：
```json
{
  "code": "0000",
  "info": "成功",
  "data": [
    {
      "appName": "myAgent",
      "agentId": "100001",
      "agentName": "我的智能体",
      "agentDesc": "描述",
      "baseUrl": "...",
      "apiKey": "...",
      "model": "gpt-4.1",
      "agents": [...],
      "runnerAgentName": "myAgent",
      "pluginNameList": [...]
    }
  ]
}
```

**DELETE `/agent-config/delete/{appName}`**

响应：
```json
{ "code": "0000", "info": "配置已删除，重启后生效", "data": null }
```

### 智能体配置 API 调用方法

```typescript
// api/agentConfig.ts
import http from './http';
import type { ApiResponse } from './types';

export interface McpToolConfig {
  type: 'sse' | 'stdio' | 'local';
  name: string;
  baseUri?: string;
  sseEndpoint?: string;
  requestTimeout?: number;
  command?: string;
  args?: string[];
  env?: Record<string, string>;
}

export interface AgentDefinition {
  name: string;
  instruction: string;
  description: string;
  outputKey?: string;
}

export interface WorkflowConfig {
  type: 'loop' | 'parallel' | 'sequential';
  name: string;
  subAgents: string[];
  description: string;
  maxIterations: number;
}

export interface AgentConfigForm {
  appName: string;
  agentId: string;
  agentName: string;
  agentDesc: string;
  baseUrl: string;
  apiKey: string;
  completionsPath?: string;
  embeddingsPath?: string;
  model: string;
  toolMcpList: McpToolConfig[];
  agents: AgentDefinition[];
  agentWorkflows: WorkflowConfig[];
  runnerAgentName: string;
  pluginNameList: string[];
}

/** 保存智能体配置 */
export function saveAgentConfig(config: AgentConfigForm) {
  return http.post<ApiResponse<string>>('/agent-config/save', config);
}

/** 获取已配置智能体列表 */
export function listAgentConfigs() {
  return http.get<ApiResponse<AgentConfigForm[]>>('/agent-config/list');
}

/** 删除智能体配置 */
export function deleteAgentConfig(appName: string) {
  return http.delete<ApiResponse<null>>(`/agent-config/delete/${appName}`);
}
```

### Axios 封装要求

```typescript
// 必须实现的拦截器
- 请求拦截：自动注入通用 headers
- 响应拦截：统一处理 code !== '0000' 的业务错误（toast 通知）
- 错误拦截：网络错误、超时统一处理
```

### 流式 SSE 处理

```typescript
// 使用 fetch API（不是 EventSource，因为是 POST 方法）
async function chatStream(params: ChatRequest, onChunk: (text: string) => void) {
  const response = await fetch(`${API_BASE}/chat_stream`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(params)
  });
  const reader = response.body!.getReader();
  const decoder = new TextDecoder();
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    onChunk(decoder.decode(value, { stream: true }));
  }
}
```

### 业务错误码处理

| code | 处理方式 |
|------|----------|
| `0000` | 正常处理 |
| `0001` | toast 警告「未知错误，请重试」 |
| `0002` \| `0004` | toast 错误「请求参数异常」 |
| `E0001` | toast 错误「Agent 不存在，请返回重新选择」+ 跳转首页 |
| `E0002` | toast 警告「Agent MCP 配置加载失败，功能可能受限」|

---

## 📁 项目目录结构

```
src/
├── api/
│   ├── agent.ts          # Agent 列表、Session 创建
│   ├── chat.ts           # 普通聊天、流式聊天
│   ├── http.ts           # Axios 实例与拦截器
│   └── types.ts          # 全量 TypeScript 类型定义
│
├── assets/
│   ├── styles/
│   │   ├── variables.css  # CSS 变量（上文定义的全套）
│   │   ├── global.css     # 全局重置 + 滚动条样式 + 选中色
│   │   └── element.css    # Element Plus 主题覆盖（全暗色）
│   └── fonts.css          # Google Fonts 引入
│
├── components/
│   ├── layout/
│   │   ├── TopNav.vue         # 顶部导航（Logo + 用户ID + 状态灯）
│   │   └── SideBar.vue        # 左侧 Agent 列表导航
│   ├── agent/
│   │   ├── AgentCard.vue      # 首页 Agent 卡片
│   │   └── ConfigForm.vue     # 智能体配置表单（Agent Designer 核心组件）
│   └── chat/
│       ├── MessageList.vue    # 消息流容器（虚拟滚动备用）
│       ├── MessageBubble.vue  # 单条消息（区分 user/ai/tool）
│       ├── MarkdownRenderer.vue # markdown-it 渲染器
│       ├── ChatInput.vue      # 底部输入区（含模式切换）
│       ├── StreamCursor.vue   # 闪烁光标组件
│       └── EmptyState.vue     # 空状态（ASCII art + 建议问题）
│
├── views/
│   ├── AgentListView.vue  # 首页 Dashboard
│   ├── ChatView.vue       # 聊天工作台
│   └── AgentDesigner.vue  # 智能体配置设计器页面
│
├── stores/
│   ├── userStore.ts    # userId 管理（localStorage 持久化）
│   ├── agentStore.ts   # Agent 列表缓存
│   └── chatStore.ts    # 当前会话、消息列表、sessionId
│
├── utils/
│   ├── sse.ts          # 流式请求封装
│   ├── markdown.ts     # markdown-it 实例配置（含 highlight.js）
│   └── format.ts       # 时间格式化、token 估算等工具函数
│
├── App.vue             # 根组件（全局背景、字体加载）
├── main.ts             # 入口（Element Plus 按需 + Pinia + Router）
└── router.ts           # 路由（/ → AgentList，/chat/:agentId → ChatView，/designer → AgentDesigner，/designer/:appName → AgentDesigner 编辑模式）
```

---

## 🔧 TypeScript 类型定义

```typescript
// api/types.ts

interface ApiResponse<T> {
  code: string;
  info: string;
  data: T;
}

interface AgentConfig {
  agentId: string;
  agentName: string;
  agentDesc: string;
}

interface CreateSessionResponse {
  sessionId: string;
}

interface ChatRequest {
  agentId: string;
  userId: string;
  sessionId: string;
  message: string;
}

interface ChatResponse {
  content: string;
}

// 智能体配置（Agent Designer 相关）
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

interface AgentConfigForm {
  appName: string;
  agentId: string;
  agentName: string;
  agentDesc: string;
  baseUrl: string;
  apiKey: string;
  completionsPath?: string;
  embeddingsPath?: string;
  model: string;
  toolMcpList: McpToolConfig[];
  agents: AgentDefinition[];
  agentWorkflows: WorkflowConfig[];
  runnerAgentName: string;
  pluginNameList: string[];
}

// 消息类型（前端内部使用）
type MessageRole = 'user' | 'assistant' | 'tool';

interface Message {
  id: string;
  role: MessageRole;
  content: string;
  timestamp: number;
  isStreaming?: boolean;   // AI 消息流式进行中
  toolName?: string;       // role === 'tool' 时的工具名称
  error?: boolean;         // 是否是错误消息
}
```

---

## 🎬 动画规范

| 场景 | 动画 | 时长 | 曲线 |
|------|------|------|------|
| 页面进入 | fade + translateY(20px) | 500ms | ease-out-expo |
| 卡片入场 | stagger fade-in-up（每张 +80ms） | 400ms | ease-out-expo |
| 消息出现 | fade + translateY(8px) | 300ms | ease-out |
| 卡片 hover | translateY(-4px) + glow | 200ms | ease |
| 按钮 hover | scale(1.02) + brightness | 150ms | ease |
| 侧边栏激活 | 左边线从 0→3px | 200ms | ease |
| 模式切换 toggle | 滑块平移 | 250ms | ease-in-out-quart |
| Loading 三点 | 依次 scale(0.6→1.2→0.6) | 1200ms loop | ease-in-out |
| 光标闪烁 | opacity 0→1→0 | 800ms loop | step-end |

**滚动条全局样式**（Webkit）：
```css
::-webkit-scrollbar { width: 4px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--border-default); border-radius: 2px; }
::-webkit-scrollbar-thumb:hover { background: var(--accent-primary); }
```

---

## 📱 响应式断点

| 断点 | 宽度 | 布局变化 |
|------|------|----------|
| Desktop | ≥1280px | 三栏布局（聊天页）+ 左侧固定侧边栏 |
| Laptop | 1024-1279px | 聊天页右侧面板隐藏 + 侧边栏可折叠 |
| Tablet | 768-1023px | 侧边栏变为顶部 Tab 导航 |
| Mobile | <768px | 单栏，侧边栏变为抽屉 |

---

## ⚠️ 开发注意事项

1. **Element Plus 主题必须完全覆盖**：所有 el-* 组件的默认白色/浅色必须通过 CSS 变量覆盖成暗色，禁止出现默认白色弹窗、白色下拉菜单

2. **禁止使用以下设计**：
    - 白色/浅灰背景
    - 默认蓝色（`#409EFF`，Element Plus 默认色）
    - Inter / Roboto / Arial 字体
    - 紫色渐变在白底上（陈词滥调）
    - 无边框无分层的扁平卡片

3. **流式接口注意事项**：
    - `/chat_stream` 是 POST + ReadableStream，不能用 EventSource
    - 首次请求可能需要 3-10 秒初始化，必须有明确 loading 提示
    - 流式中断需要优雅处理（reader.cancel()）

4. **userId 管理**：
    - 生成规则：`usr_` + `nanoid(8)`
    - 存储：`localStorage.setItem('agent_user_id', userId)`
    - 首次访问时弹出确认 Modal，允许用户自定义

5. **Markdown 安全**：使用 `markdown-it` 时开启 `html: false`，防止 XSS

6. **消息列表性能**：消息超过 100 条时考虑只渲染可视区域（虚拟列表）

---

## 🚀 环境配置

```typescript
// src/config.ts
export const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8091/api/v1';
```

```env
# .env.development
VITE_API_BASE=http://localhost:8091/api/v1
```

---

*设计理念：这个平台是开发者与 AI 智能体协作的专业工具，每一个细节都应该传达精密、可信赖、高效的感受。像 Linear、Vercel Dashboard 一样——用户打开它就知道这是一个严肃、优秀的产品。*