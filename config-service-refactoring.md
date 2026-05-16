# Config 智能体配置服务 — 重构笔记

## 背景

原有 `AgentConfigService` 是平铺直叙的 CRUD 实现：

- `convertDomainToYaml()` 内联构建 LinkedHashMap 50+ 行
- `buildTableVO()` 内联构建 VO 80+ 行
- 文件 IO 直接写在 Service 里
- 接口位置随意，不在 `domain/agent/` 层级

与项目中 armory 链（RootNode → AiApiNode → ChatModelNode → AgentNode → RunnerNode）的风格严重不一致。

## 设计目标

1. **责任链模式** 编排 save 流程：校验 → 装配 → 持久化 → 热装配
2. **Command 实体** 包装请求参数
3. **DynamicContext** 在链节点间传递状态
4. **Factory** 提供链入口 + 上下文定义
5. **DDD 分层**：接口在 domain/，仓库/装配器接口在 domain/，实现分离
6. **包结构对齐**：`service/config/` 只放 Service（同 `service/chat/`），所有支撑代码归入 `service/armory/`

---

## 最终包结构

```
domain/agent/
├── IConfigService.java                         ← 领域接口
├── model/entity/
│   └── ConfigSaveCommandEntity.java            ← 命令实体
└── service/
    ├── config/
    │   └── ConfigService.java                  ← 仅此一个文件
    └── armory/
        ├── AbstractConfigSupport.java           ← 链基类
        ├── ConfigFactory.java                   ← 工厂 + DynamicContext
        ├── assembly/
        │   ├── IConfigAssembler.java            ← 装配接口
        │   └── ConfigAssembler.java             ← YAML/VO 互转
        ├── repository/
        │   ├── IConfigRepository.java           ← 仓储接口
        │   └── ConfigRepositoryImpl.java        ← 文件实现
        └── node/
            ├── ConfigValidateNode.java          ← 校验节点
            ├── ConfigAssemblyNode.java          ← 装配节点
            ├── ConfigPersistenceNode.java       ← 持久化节点
            └── ConfigHotReloadNode.java         ← 热装配节点
```

---

## 代码实现详解

### 1. 领域接口 — `IConfigService.java`

```java
package org.example.domain.agent;

public interface IConfigService {
    String saveAgentConfig(ConfigSaveCommandEntity command) throws Exception;
    List<AgentConfig> listAgentConfigs();
    void deleteAgentConfig(String appName) throws Exception;
}
```

与 `IChatService` 同级，使用 `AgentConfig`（领域对象）而非 DTO。

### 2. 命令实体 — `ConfigSaveCommandEntity.java`

```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfigSaveCommandEntity {
    private AgentConfig agentConfig;
}
```

与 `ArmoryCommandEntity` 同级同模式，封装 save 操作的入参。

### 3. 链基类 — `AbstractConfigSupport.java`

```java
public abstract class AbstractConfigSupport
    extends AbstractMultiThreadStrategyRouter<
        ConfigSaveCommandEntity,
        ConfigFactory.DynamicContext,
        String> {

    @Override
    protected void multiThread(...) {
        // 空实现，同 AbstractArmorySupport
    }
}
```

- 三个泛型参数：**请求类型**、**上下文类型**、**返回类型**
- 节点继承它后只需实现 `doApply()`（执行逻辑）和 `get()`（路由到下一节点）

### 4. 工厂 + 上下文 — `ConfigFactory.java`

```java
@Service
public class ConfigFactory {

    @Resource
    private ConfigValidateNode configValidateNode;

    private static StrategyHandler<ConfigSaveCommandEntity, DynamicContext, String> strategyHandler;

    @PostConstruct
    public void init() {
        strategyHandler = configValidateNode;  // 链入口
    }

    public static StrategyHandler<...> configStrategyHandler() {
        return strategyHandler;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        private String yamlContent;
        private AiAgentConfigTableVO tableVO;
        private String filename;
    }
}
```

关键点：

- `@PostConstruct init()` 将 `ConfigValidateNode` 注册为链的入口
- 后续节点通过 `@Resource` 注入下一个节点实现链式链接
- `DynamicContext` 作为链节点的数据总线，每个节点从中读取/写入

### 5. 链节点 — ConfigValidateNode

```java
@Slf4j
@Service
public class ConfigValidateNode extends AbstractConfigSupport {

    @Resource
    private ConfigAssemblyNode configAssemblyNode;

    @Override
    protected String doApply(ConfigSaveCommandEntity req, ConfigFactory.DynamicContext ctx) throws Exception {
        AgentConfig config = req.getAgentConfig();
        Assert.hasText(config.getAppName(), "appName 不能为空");
        // ... 其他校验
        return router(req, ctx);  // 路由到下一节点
    }

    @Override
    public StrategyHandler<...> get(...) throws Exception {
        return configAssemblyNode;  // 指向装配节点
    }
}
```

节点模式：
1. `doApply()` — 执行当前节点的逻辑
2. 调用 `router()` — 触发 `get()` 找到下一节点，执行 `apply()`
3. `get()` — 返回下一节点 handler

### 6. 链节点 — ConfigAssemblyNode

```java
@Override
protected String doApply(ConfigSaveCommandEntity req, ConfigFactory.DynamicContext ctx) throws Exception {
    AgentConfig config = req.getAgentConfig();

    // 装配 YAML → 写入上下文
    ctx.setYamlContent(configAssembler.toYaml(config));
    // 装配 TableVO → 写入上下文（供热装配用）
    ctx.setTableVO(configAssembler.toTableVO(config));

    return router(req, ctx);
}
```

职责单一：只做转换，不做文件 IO 或热装配。

### 7. 链节点 — ConfigPersistenceNode

```java
@Override
protected String doApply(...) throws Exception {
    String filename = configRepository.save(
        req.getAgentConfig().getAppName(),
        ctx.getYamlContent()
    );
    ctx.setFilename(filename);
    return router(req, ctx);
}
```

通过 `IConfigRepository` 接口解耦文件系统，不关心具体实现。

### 8. 链节点 — ConfigHotReloadNode（终点）

```java
@Override
protected String doApply(...) throws Exception {
    armoryService.acceptArmoryAgents(Collections.singletonList(ctx.getTableVO()));
    return ctx.getFilename();  // 返回结果，不再 router
}

@Override
public StrategyHandler<...> get(...) throws Exception {
    return defaultStrategyHandler;  // 链终点
}
```

- 调用 `IArmoryService.acceptArmoryAgents()` 触发 armory 装配管线
- `get()` 返回 `defaultStrategyHandler`（即 `StrategyHandler.DEFAULT`），终结链

### 9. 装配器 — ConfigAssembler

```java
@Component
public class ConfigAssembler implements IConfigAssembler {

    private final ObjectMapper yamlMapper;

    public ConfigAssembler() {
        YAMLFactory yamlFactory = new YAMLFactory()
            .configure(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR, true);
        this.yamlMapper = new ObjectMapper(yamlFactory);
    }

    public String toYaml(AgentConfig config) {
        // 构建 LinkedHashMap 层级结构 → yamlMapper.writeValueAsString(root)
    }

    public AiAgentConfigTableVO toTableVO(AgentConfig config) {
        // 手动映射 AgentConfig → AiAgentConfigTableVO（每个嵌套类）
    }

    public AgentConfig fromTableVO(AiAgentConfigTableVO vo) {
        // 反向映射（用于 list 查询）
    }
}
```

为什么不用 Jackson 注解自动序列化？因为 YAML 结构（`ai.agent.config.tables.{appName}.module...`）与 AgentConfig 类结构不一致，手动映射更可控。

### 10. 仓储 — ConfigRepositoryImpl

```java
@Repository
public class ConfigRepositoryImpl implements IConfigRepository {

    @Override
    public String save(String appName, String yamlContent) throws Exception {
        String filename = appName + ".yml";
        Path outputDir = Paths.get("data/agent-configs");
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve(filename), yamlContent,
            StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return filename;
    }

    @Override
    public void delete(String appName) throws Exception {
        Files.deleteIfExists(Paths.get("data/agent-configs", appName + ".yml"));
    }
}
```

纯基础设施，与领域逻辑完全分离。

### 11. ConfigService（入口）

```java
@Service
public class ConfigService implements IConfigService {

    @Resource
    private AiAgentAutoConfigProperties aiAgentAutoConfigProperties;
    @Resource
    private IConfigAssembler configAssembler;
    @Resource
    private IConfigRepository configRepository;

    @Override
    public String saveAgentConfig(ConfigSaveCommandEntity command) throws Exception {
        StrategyHandler<...> handler = ConfigFactory.configStrategyHandler();
        return handler.apply(command, new ConfigFactory.DynamicContext());
    }

    @Override
    public List<AgentConfig> listAgentConfigs() {
        if (aiAgentAutoConfigProperties.getTables() == null) return Collections.emptyList();
        return aiAgentAutoConfigProperties.getTables().values().stream()
                .map(configAssembler::fromTableVO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAgentConfig(String appName) throws Exception {
        configRepository.delete(appName);
    }
}
```

save 走责任链，list/delete 直接调用装配器/仓储——链模式只用于多步骤的 save 流程。

---

## 链调用时序

```
Controller
  │ saveAgentConfig(ConfigSaveCommandEntity)
  ▼
ConfigFactory.configStrategyHandler().apply(command, new DynamicContext())
  │
  ▼
ConfigValidateNode.doApply()      ← 校验参数，写入 ctx，router()
  │
  ▼
ConfigAssemblyNode.doApply()      ← configAssembler.toYaml() + toTableVO()，router()
  │
  ▼
ConfigPersistenceNode.doApply()   ← configRepository.save()，router()
  │
  ▼
ConfigHotReloadNode.doApply()     ← armoryService.acceptArmoryAgents()，返回 filename
```

每个节点通过 `@Resource` 持有下一个节点的引用，`get()` 返回该引用，`router()` 调用 `get()` 并执行 `apply()`。

---

## 与 armory 链的对比

| | armory 链 | config 链 |
|---|---|---|
| 基类 | `AbstractArmorySupport` | `AbstractConfigSupport` |
| 工厂 | `DefaultArmoryFactory` | `ConfigFactory` |
| 上下文 | `DynamicContext`（OpenAiApi, ChatModel, AgentGroup...） | `DynamicContext`（yamlContent, tableVO, filename） |
| 链入口 | `RootNode` | `ConfigValidateNode` |
| 链终点 | `RunnerNode`（注册 Bean） | `ConfigHotReloadNode`（调用 IArmoryService） |
| 返回类型 | `AiAgentRegisterVO` | `String`（文件名） |

两套链共享同一套 `tree/` 抽象（`AbstractStrategyRouter`、`AbstractMultiThreadStrategyRouter`、`StrategyHandler`、`StrategyMapper`）。

---

## 清理的死代码

- `trigger/http/service/IAgentConfigService.java` — 未被引用
- `trigger/http/service/AgentConfigService.java` — 相同逻辑的重复拷贝

两文件在 domain 层已有对应实现，Controller 也从未引用它们。
