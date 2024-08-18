package com.atchen.AISearch.controller;

import com.atchen.AISearch.entity.Answer;
import com.atchen.AISearch.entity.enums.AiModelEnum;
import com.atchen.AISearch.entity.enums.AiTypeEnum;
import com.atchen.AISearch.service.IAnswerService;
import com.atchen.AISearch.utils.AppVariable;
import com.atchen.AISearch.utils.ResultEntity;
import com.atchen.AISearch.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.volcengine.ark.runtime.service.ArkService;

import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-15
 * @Description: 字节大模型接入
 * @Version: 1.0
 */
@RestController
@RequestMapping("/doubao")
public class DoubaoController {
    @Value("${doubao.api-key}")
    private String apiKey;
    @Value("${doubao.url}")
    private String url;
    @Value("${doubao.model-id}")
    private String modelId;
    @Resource
    private IAnswerService   answerService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private KafkaTemplate kafkaTemplate;


    /*
        * @description  对话功能
        * @author atchen
        * @date 2024/8/15
    */
    @PostMapping("/chat")
    public ResultEntity chat(String question) {
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        String result = "";  // 对话结果
        boolean save = false; // 是否保存对话记录
        // 分布式锁
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.DOUBAO.getValue(), AiTypeEnum.CHAT.getValue());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(30, TimeUnit.SECONDS);
            if (!tryLock) {
                return ResultEntity.fail("请求过于频繁，请稍后重试！");
            }
            // 业务逻辑
        // 1 apikey url
        ArkService service = ArkService.builder().apiKey(apiKey)
                .baseUrl(url).build();
        // 2 构建请求参数
        List<ChatMessage> messages =  new ArrayList<>();
        messages.add(ChatMessage.builder()
                        .role(ChatMessageRole.USER)
                        .content(question)
                        .build());
        // 3 构建请求对象
        ChatCompletionRequest request = ChatCompletionRequest.builder()
               .model(modelId) //接入点的id
               .messages(messages)
               .build();
        // 4 解析返回结果
        result = service.createChatCompletion(request).getChoices()
                .get(0)   //获取第一个消息
                .getMessage()
                .getContent().toString();
        Answer answer = new Answer();
        answer.setUid(SecurityUtil.getUserDetails().getUid());
        answer.setTitle(question);
        answer.setContent(result);
        answer.setType(AiTypeEnum.CHAT.getValue());
        answer.setModel(AiModelEnum.DOUBAO.getValue());
        save = answerService.save(answer);
        }catch (Exception e){}
        finally {
            lock.unlock();
        }
        if (save){
            // 发送消息到kafka 扣减次数
            kafkaTemplate.send(AppVariable.SUB_USECOUNT_TOPIC,uid.toString());
            return ResultEntity.success(result);
        }

        else return ResultEntity.fail("请求失败,请稍后重试！");
    }
    // 查询历史对话列表
    @PostMapping("getchatlist")
    public ResultEntity getChatList() {
        Long uid = SecurityUtil.getUserDetails().getUid();
        int type = AiTypeEnum.CHAT.getValue();
        int model = AiModelEnum.DOUBAO.getValue();
        String cacheKey = AppVariable.getListCacheKey(uid,model,type);  // 缓存key
        Object list = redisTemplate.opsForValue().get(cacheKey);
        if (list == null){
            List<Answer> answers= answerService.list(Wrappers
                    .lambdaQuery(Answer.class)
                    .eq(Answer::getUid, uid)
                    .eq(Answer::getType, type)
                    .eq(Answer::getModel, model)
                    .orderByDesc(Answer::getAid)
            );
            redisTemplate.opsForValue().set(cacheKey,answers,1, TimeUnit.DAYS);
            return ResultEntity.success(answers);
        }else {
            return ResultEntity.success(list);
        }
    }
}
    