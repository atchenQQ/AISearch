package com.atchen.AISearch.controller;

import cn.hutool.core.lang.UUID;
import com.alibaba.cloud.ai.tongyi.chat.TongYiChatClient;
import com.alibaba.cloud.ai.tongyi.chat.TongYiChatOptions;
import com.alibaba.cloud.ai.tongyi.image.TongYiImagesClient;

import com.atchen.AISearch.entity.Answer;
import com.atchen.AISearch.entity.enums.AiModelEnum;
import com.atchen.AISearch.entity.enums.AiTypeEnum;
import com.atchen.AISearch.service.IAnswerService;
import com.atchen.AISearch.utils.AppVariable;
import com.atchen.AISearch.utils.MinioUtil;
import com.atchen.AISearch.utils.ResultEntity;
import com.atchen.AISearch.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-12
 * @Description: 通义大模型接口
 * @Version: 1.0
 */

@RestController
@RequestMapping("/tongyi")
public class TongyiController {
    @Resource
    private TongYiChatClient chatClient;
    @Resource
    private TongYiImagesClient imagesClient;
    @Resource
    private IAnswerService answerService;
    @Resource
    private MinioUtil minioUtil;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private KafkaTemplate kafkaTemplate;
    @Resource
    private RedisTemplate redisTemplate;
    /*
        * @description 聊天功能
        * @author atchen
        * @date 2024/8/12
    */
    @PostMapping("/chat")
    public ResultEntity chat(String question) {
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        String result = ""; // 结果
        boolean saveResult = false; // 保存结果
        // 分布式锁
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.TONGYI.getValue(), AiTypeEnum.CHAT.getValue());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(30, TimeUnit.SECONDS);
            if (!tryLock){
                return ResultEntity.fail("请求过于频繁，请稍后再试！");
            }
            // 业务逻辑
            TongYiChatOptions chatOptions = new TongYiChatOptions();
            result = chatClient.call(new Prompt(question,chatOptions))
                    .getResult()
                    .getOutput()
                    .getContent();
            Answer answer = new Answer();
            answer.setTitle(question);
            answer.setContent(result);
            answer.setType(AiTypeEnum.CHAT.getValue());
            answer.setModel(AiModelEnum.TONGYI.getValue());
            answer.setUid(SecurityUtil.getUserDetails().getUid());
            saveResult = answerService.save(answer);
        }catch (Exception e) {}
        finally {
            lock.unlock();
        }
        if (saveResult){
            // 发送消息到kafka 扣减余额
            kafkaTemplate.send(AppVariable.SUB_USECOUNT_TOPIC,uid.toString());
            return ResultEntity.success(result);
        }else {
            return ResultEntity.fail("操作失败，请稍后再试！");
        }
    }
    /*  绘画
        * @description
        * @author atchen
        * @date 2024/8/12 18:14
    */
    @PostMapping("/draw")
    public ResultEntity draw(String question) {
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        String url = "";
        boolean saveResult = false; // 保存结果
        // 分布式锁
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.TONGYI.getValue(), AiTypeEnum.DRAW.getValue());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(30, TimeUnit.SECONDS);
            if (!tryLock){
                return ResultEntity.fail("请求过于频繁，请稍后再试！");
            }
            // 业务逻辑
            Image image = imagesClient.call(new ImagePrompt(question))
                     .getResult().getOutput();
            String b64Json = image.getB64Json();

            try( InputStream inputStream =  new ByteArrayInputStream(
                    Base64.getDecoder().decode(b64Json)
            )) {
                String fileName = "ty-"+ UUID.randomUUID().toString().replaceAll("-","");
              url =  minioUtil.upload(fileName, inputStream,"image/png");
            } catch (Exception e) { return ResultEntity.fail("操作失败，请稍后再试！"); }

            Answer answer = new Answer();
            answer.setTitle(question);
            answer.setContent(url);
            answer.setType(AiTypeEnum.DRAW.getValue());
            answer.setModel(AiModelEnum.TONGYI.getValue());
            answer.setUid(SecurityUtil.getUserDetails().getUid());
            saveResult = answerService.save(answer);
        }catch (Exception e) {}
        finally {
            lock.unlock();
        }
        if (saveResult){
            // 发送消息到kafka 扣减余额
            kafkaTemplate.send(AppVariable.SUB_USECOUNT_TOPIC,uid.toString());
            return ResultEntity.success(url);
        }else {
            return ResultEntity.fail("操作失败，请稍后再试！");
        }
    }


    /*
        * @description   通义历史记录
        * @author atchen
        * @date 2024/8/12
    */
    @PostMapping("/getchatlist")
    public ResultEntity getChatList() {
        Long uid = SecurityUtil.getUserDetails().getUid();
        int type = AiTypeEnum.CHAT.getValue();
        int model =AiModelEnum.TONGYI.getValue();
        String cacheKey = AppVariable.getListCacheKey(uid,model,type);  // 缓存key
        Object list = redisTemplate.opsForValue().get(cacheKey);
        if (list== null){
            // 缓存中没有数据，从数据库中查询
            QueryWrapper<Answer> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("uid", uid)
                    .eq("type",type)
                    .eq("model", model)
                    .orderByDesc("aid");
            List<Answer> answers = answerService.list(queryWrapper);
            redisTemplate.opsForValue().set(cacheKey,answers,1, TimeUnit.DAYS);
            return ResultEntity.success(answers);
        }
        return ResultEntity.success(list);
    }
    /*
        * @description    获取绘画历史信息
        * @author atchen
        * @date 2024/8/12
    */
    @PostMapping("/getdrawlist")
    public ResultEntity getDrawList() {
        Long uid = SecurityUtil.getUserDetails().getUid();
        int type = AiTypeEnum.DRAW.getValue();
        int model =AiModelEnum.TONGYI.getValue();
        String cacheKey = AppVariable.getListCacheKey(uid,model,type);  // 缓存key
        Object list = redisTemplate.opsForValue().get(cacheKey);
        if (list== null){
            // 缓存中没有数据，从数据库中查询
            QueryWrapper<Answer> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("uid", uid)
                    .eq("type",type)
                    .eq("model", model)
                    .orderByDesc("aid");
            List<Answer> answers = answerService.list(queryWrapper);
            redisTemplate.opsForValue().set(cacheKey,answers,1, TimeUnit.DAYS);
            return ResultEntity.success(answers);
        }
        return ResultEntity.success(list);
    }
}

    