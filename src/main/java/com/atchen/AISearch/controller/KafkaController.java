package com.atchen.AISearch.controller;

import com.atchen.AISearch.entity.Answer;
import com.atchen.AISearch.service.IUserService;
import com.atchen.AISearch.utils.AppVariable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-16
 * @Description: 事件监听canal同步redis
 * @Version: 1.0
 */

@RestController
@RequestMapping("/kafka")
public class KafkaController {
    @Resource
    private KafkaTemplate kafkaTemplate;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private IUserService userService;
    private  static final String CANAL_TOPIC = "ai-search-canal-to-kafka";  // canal同步的topic名称
    private  static final String SUB_USECOUNT_TOPIC = "sub-user-usecount";  // 订阅用户模型使用次数的topic名称


    // 监听canal同步redis
    @KafkaListener(topics = {CANAL_TOPIC})
    public void canalListen(String data, Acknowledgment acknowledgment) throws JsonProcessingException {
        HashMap<String, Object> map = objectMapper.readValue(data, HashMap.class);
        if (!map.isEmpty() && map.get("database").toString().equals("aisearch") &&
                map.get("table").toString().equals("answer")) {
            // 更新 Redis 缓存
            ArrayList<LinkedHashMap<String, Object>> list =
                    (ArrayList<LinkedHashMap<String, Object>>) map.get("data");
            String cacheKey = "";
            for (LinkedHashMap<String, Object> answer : list) {
                cacheKey = AppVariable.getListCacheKey(
                        Long.parseLong(answer.get("uid").toString()),
                        Integer.parseInt(answer.get("model").toString()),
                        Integer.parseInt(answer.get("type").toString()));
                redisTemplate.opsForValue().set(cacheKey, null);
            }
        }
        // 手动确认应答
        acknowledgment.acknowledge();
    }
    // 监听订阅用户模型扣减使用次数的topic
    @KafkaListener(topics = {SUB_USECOUNT_TOPIC})
    public void subUsecountListen(String uid, Acknowledgment acknowledgment) throws JsonProcessingException {
        // 扣减用户模型使用次数

        int result = userService.updateUsecount(Long.parseLong(uid));
        if (result > 0){
            // 手动确认应答
            acknowledgment.acknowledge();
        }
    }

}

    