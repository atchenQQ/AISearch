package com.atchen.AISearch.controller;

import cn.hutool.core.lang.UUID;
import com.atchen.AISearch.entity.Answer;
import com.atchen.AISearch.entity.enums.AiModelEnum;
import com.atchen.AISearch.entity.enums.AiTypeEnum;
import com.atchen.AISearch.service.IAnswerService;
import com.atchen.AISearch.utils.AppVariable;
import com.atchen.AISearch.utils.MinioUtil;
import com.atchen.AISearch.utils.ResultEntity;
import com.atchen.AISearch.utils.SecurityUtil;
import com.baidubce.qianfan.Qianfan;
import com.baidubce.qianfan.model.chat.ChatResponse;
import com.baidubce.qianfan.model.image.Text2ImageResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.minio.errors.*;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-13
 * @Description: 百度大模型接入
 * @Version: 1.0
 */


@RestController
@RequestMapping("/qianfan")
public class QianfanController {
    @Value("${qianfan.api-key}")
    private String apiKey;
    @Value("${qianfan.secret-key}")
    private String secretKey;
    @Resource
    private IAnswerService   answerService;
    @Resource
    private MinioUtil minioUtil;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private KafkaTemplate kafkaTemplate;
    @Resource
    private RedisTemplate redisTemplate;
    /*
        * @description  聊天功能
        * @author atchen
        * @date 2024/8/13
    */
    @PostMapping("/chat")
    public ResultEntity chat(String question) {
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        String content = "";  // 保存聊天内容
        boolean saveResult = false;  // 保存聊天记录结果
        // 分布式锁
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.QIANFAN.getValue(), AiTypeEnum.CHAT.getValue());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(30, TimeUnit.SECONDS);
            if (!tryLock) {
                return ResultEntity.fail("操作过于频繁，请稍后再试！");
            }
            // 业务逻辑
            ChatResponse response = new Qianfan(apiKey, secretKey).chatCompletion()
                .model("ERNIE-Speed-8K") // 使用 model 指定预置模型
                .addMessage("user", question) // 添加用户消息 (此方法可以调用多次，以实现多轮对话的消息传递)
                .temperature(0.7)
                .execute(); // 发起请求
            content  = response.getResult();
            // 保存到数据库
            Answer answer = new Answer();
            answer.setTitle(question);
            answer.setContent(content);
            answer.setModel(AiModelEnum.QIANFAN.getValue());
            answer.setType(AiTypeEnum.CHAT.getValue());
            answer.setUid(SecurityUtil.getUserDetails().getUid());
            saveResult = answerService.save(answer);
        }catch (Exception e){}
        finally {
            lock.unlock();
        }
        if (saveResult){
            // 发送消息到kafka 扣减余额
            kafkaTemplate.send(AppVariable.SUB_USECOUNT_TOPIC,uid.toString());
            return ResultEntity.success(content);
        }
        return ResultEntity.fail("操作失败，请稍后再试！");
    }
    /*
        * @description   绘图功能
        * @author atchen
        * @date 2024/8/13
    */
    @PostMapping("/draw")
    public ResultEntity draw(String question) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        String url = ""; // 保存图片地址
        boolean saveResult = false;  // 保存聊天记录结果
        // 分布式锁
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.QIANFAN.getValue(), AiTypeEnum.DRAW.getValue());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(30, TimeUnit.SECONDS);
            if (!tryLock) {
                return ResultEntity.fail("操作过于频繁，请稍后再试！");
            }
            // 业务逻辑
            Text2ImageResponse response = new Qianfan(apiKey, secretKey)
                .text2Image()
                .prompt(question)
                .execute();
            byte[] image = response.getData().get(0).getImage();
            String fileName = "qf-" + UUID.randomUUID().toString().replace("-", "");
            try( InputStream inputStream = new ByteArrayInputStream(image)) {
               url = minioUtil.upload(fileName,inputStream,"image/png");
            }
            // 保存到数据库
            Answer answer = new Answer();
            answer.setTitle(question);
            answer.setContent(url);
            answer.setModel(AiModelEnum.QIANFAN.getValue());
            answer.setType(AiTypeEnum.DRAW.getValue());
            answer.setUid(SecurityUtil.getUserDetails().getUid());
            saveResult = answerService.save(answer);
        }catch (Exception e){}
        finally {
            lock.unlock();
        }
        if (saveResult){
            // 发送消息到kafka 扣减余额
            kafkaTemplate.send(AppVariable.SUB_USECOUNT_TOPIC,uid.toString());
            return ResultEntity.success(url);     //todo  暂未开通付费
        }
        return ResultEntity.fail("文心未开通付费操，请付费再试！");
    }

    /*
        * @description 获取历史记录信息
        * @author atchen
        * @date 2024/8/13 19:25
    */
    @PostMapping("/getchatlist")
    public ResultEntity getChatList() {
        Long uid = SecurityUtil.getUserDetails().getUid();
        int type = AiTypeEnum.CHAT.getValue();
        int model = AiModelEnum.QIANFAN.getValue();
        String cacheKey = AppVariable.getListCacheKey(uid,model,type);  // 缓存key
        Object list = redisTemplate.opsForValue().get(cacheKey);
        if (list == null){
            // 缓存中没有数据，从数据库中查询
            QueryWrapper<Answer> queryWrapper = new QueryWrapper();
            queryWrapper.eq("uid", uid);
            queryWrapper.eq("model", model);
            queryWrapper.eq("type", type);
            queryWrapper.orderByDesc("aid");
            List<Answer> answers = answerService.list(queryWrapper);
            redisTemplate.opsForValue().set(cacheKey,answers,1, TimeUnit.DAYS);
            return ResultEntity.success(answers);
        }else{
            return ResultEntity.success(list);
        }
    }

    /*
        * @description 获取绘图记录信息
        * @author atchen
        * @date 2024/8/13
    */
    @PostMapping("/getdrawlist")
    public ResultEntity getDrawList() {
        Long uid = SecurityUtil.getUserDetails().getUid();
        int type = AiTypeEnum.DRAW.getValue();
        int model = AiModelEnum.QIANFAN.getValue();
        String cacheKey = AppVariable.getListCacheKey(uid,model,type);  // 缓存key
        Object list = redisTemplate.opsForValue().get(cacheKey);
        if (list == null){
            // 缓存中没有数据，从数据库中查询
            QueryWrapper queryWrapper = new QueryWrapper();
            queryWrapper.eq("uid", uid);
            queryWrapper.eq("model", model);
            queryWrapper.eq("type", type);
            queryWrapper.orderByDesc("aid");
            List answers = answerService.list(queryWrapper);
            redisTemplate.opsForValue().set(cacheKey,answers,1, TimeUnit.DAYS);
            return ResultEntity.success(answers);
        }else {
            return ResultEntity.success(list);
        }
    }
}
    