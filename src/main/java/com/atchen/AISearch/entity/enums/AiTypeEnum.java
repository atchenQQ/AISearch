package com.atchen.AISearch.entity.enums;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-13
 * @Description: Answer AiType 枚举: AI 工具类型
 * @Version: 1.0
 */
public enum AiTypeEnum {
    CHAT(1),
    DRAW(2);
    private int value;
    AiTypeEnum(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
    