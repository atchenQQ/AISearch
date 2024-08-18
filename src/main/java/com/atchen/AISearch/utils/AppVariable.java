package com.atchen.AISearch.utils;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-14
 * @Description: 全局变量
 * @Version: 1.0
 */


public class AppVariable {
    // 讨论表点赞Topic名称
    public static final String DISCUSS_SUPPORT_TOPIC = "DISCUSS_SUPPORT_TOPIC";
    // 订阅用户模型使用次数的topic名称
    public static final String SUB_USECOUNT_TOPIC = "sub-user-usecount";
    public static final int PAGE_SIZE = 10;   // 每页显示的数量
    // 大模型调用分布式锁的key
    public static final String getModelLockKeY(Long uid ,Integer model, Integer type){
            return "GET_MODEL_LOCK_KEY_" + uid + "_" + model + "_" + type;
    }

    public static final String getListCacheKey(Long uid, int model, int type) {
        return "LIST_CACHE_KEY_" + uid + "_" + model + "_" + type;
    }
}
    