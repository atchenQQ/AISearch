package com.atchen.AISearch.entity.enums;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-13
 * @Description: Answer AiModel 枚举 Ai大模型
 * @Version: 1.0
 */


public enum AiModelEnum {
    OPENAI(1),
    TONGYI(2),
    XUNFEI(3),
    QIANFAN(4),
    DOUBAO(5),
    LOCAL(6);
    private  int value;
    AiModelEnum(int value){
        this.value = value;
    }
    public int getValue() { return value; }
}
    