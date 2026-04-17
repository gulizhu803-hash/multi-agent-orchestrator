package org.example.domain.agent.model.valobj.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum AgentTypeEnum {
    Loop("循环执行","loop","loopAgentNode"),
    Parallel("并行执行","loop","parallelAgentNode"),
    Sequential("串行执行","loop","sequentialAgentNode")

    ;
    private String name;
    private String type;
    private String node;


    /**
     * 根据类型字符串获取对应的 AgentTypeEnum 枚举实例
     * 
     * 方法作用：
     * 1. 将外部传入的类型字符串（如 "loop"、"parallel" 等）转换为对应的枚举对象
     * 2. 提供类型安全的枚举查找机制，避免直接使用字符串比较
     * 3. 支持大小写不敏感的匹配，提高接口的容错性
     * 
     * 为什么需要这个方法：
     * 1. 在反序列化 JSON 或接收前端参数时，通常以字符串形式传递类型值
     * 2. 通过此方法可以将字符串安全地转换为枚举对象，便于后续业务逻辑处理
     * 3. 统一枚举查找逻辑，避免在多处重复编写遍历查找代码
     * 4. 当枚举值发生变化时，只需维护此处逻辑，降低维护成本
     *
     * @param type 类型字符串，对应枚举中的 type 字段值
     * @return 匹配的枚举实例，如果未找到或输入为 null 则返回 null
     */
    public static AgentTypeEnum formType(String type) {
        // 空值检查，避免 NullPointerException
        if (type == null) {
            return null;
        }

        // 遍历所有枚举值，查找 type 字段与输入字符串匹配的枚举项
        for (AgentTypeEnum value : values()) {
            // 使用忽略大小写的比较方式，增强兼容性
            if (value.getType().equalsIgnoreCase(type)) {
                return value;
            }
        }

        // 未找到匹配的枚举值时返回 null
        return null;
    }
}
