package com.atchen.AISearch.controller;
import com.atchen.AISearch.entity.Answer;
import com.atchen.AISearch.entity.enums.AiModelEnum;
import com.atchen.AISearch.entity.enums.AiTypeEnum;
import com.atchen.AISearch.service.IAnswerService;
import com.atchen.AISearch.utils.AppVariable;
import com.atchen.AISearch.utils.ResultEntity;
import com.atchen.AISearch.utils.SecurityUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-17
 * @Description: 本地ChatGpt大模型
 * @Version: 1.0
 */

@RestController
@RequestMapping("/chatgpt")
public class ChatGptController {
    @Resource
    private IAnswerService iAnswerService;
    @Resource
    private KafkaTemplate kafkaTemplate;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private OllamaChatClient ollamaChatClient;
    @PostMapping("/chat")
    public ResultEntity chat(String question, HttpServletRequest request)  {
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        // 分布式锁判断
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.OPENAI.getValue(), AiTypeEnum.CHAT.getValue());
        String call  = "";  // 聊天结果
        boolean addre = false;  // 数据添加状态
        // 1 获取分布式锁实例
        RLock lock = redissonClient.getLock(lockKey);

        try {
            //2 尝试获取锁，最多等待30秒
            boolean tryLock = lock.tryLock(30, TimeUnit.SECONDS);
            if (!tryLock) {
                return ResultEntity.fail("操作过于频繁，请稍后再试！");
            }
            // 调用openai接口进行聊天
            call = ollamaChatClient.call(question);
            //将结果保存到数据库
            Answer answer = new Answer();
            answer.setTitle(question);
            answer.setContent(call);
            answer.setModel(AiModelEnum.OPENAI.getValue());
            answer.setType(AiTypeEnum.CHAT.getValue());
            answer.setUid(SecurityUtil.getUserDetails().getUid());
            addre = iAnswerService.save(answer);
        } // try end
        catch (Exception e){

        }finally {
            // 3 释放锁
            lock.unlock();
        }
        if (addre) {
            // 发送消息到kafka 扣减余额
            kafkaTemplate.send(AppVariable.SUB_USECOUNT_TOPIC,uid.toString());
            return ResultEntity.success(call);
        }
        return ResultEntity.fail("操作失败，请稍后再试！");
    }
}
    