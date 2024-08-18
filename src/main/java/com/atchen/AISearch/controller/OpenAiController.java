package com.atchen.AISearch.controller;

import com.atchen.AISearch.entity.Answer;
import com.atchen.AISearch.entity.enums.AiModelEnum;
import com.atchen.AISearch.entity.enums.AiTypeEnum;
import com.atchen.AISearch.service.IAnswerService;
import com.atchen.AISearch.utils.AppVariable;
import com.atchen.AISearch.utils.ResultEntity;
import com.atchen.AISearch.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;

import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiImageClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;
/*
    * @description
    * @author atchen  云端ChatGpt大模型
    * @date 2024/8/17
*/

@RestController
@RequestMapping("/openai")
public class OpenAiController {

    @Resource
    private OpenAiChatClient chatModel;
    @Resource
    private OpenAiImageClient imageModel;
    @Resource
    private IAnswerService iAnswerService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private KafkaTemplate kafkaTemplate;
    /*
        * @description 聊天接口
        * @author atchen
        * @date 2024/8/9
    */
    @PostMapping("/chat")
    public ResultEntity chat(String question,HttpServletRequest request)  {
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        // 分布式锁判断
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.OPENAI.getValue(),AiTypeEnum.CHAT.getValue());
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
            call = chatModel.call(question);
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

    /*
        * @description  实现绘图功能
        * @author atchen
        * @date 2024/8/12
    */

    @PostMapping("/draw")
    public ResultEntity draw(String question) {
        String url = "";
        boolean addre = false;  // 数据添加状态
        // 调用openai接口进行绘图
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.OPENAI.getValue(),AiTypeEnum.DRAW.getValue());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(30, TimeUnit.SECONDS);
            if (!tryLock) {
                return ResultEntity.fail("操作过于频繁，请稍后再试！");
            }
            ImageResponse result = imageModel.call(new ImagePrompt(question));
            url = result.getResult().getOutput().getUrl();
            //将结果保存到数据库
            Answer answer = new Answer();
            answer.setTitle(question);
            answer.setContent(url);
            answer.setModel(AiModelEnum.OPENAI.getValue());
            answer.setType(AiTypeEnum.DRAW.getValue());
            answer.setUid(SecurityUtil.getUserDetails().getUid());
            addre = iAnswerService.save(answer);
        }catch (Exception e){}
            finally {
            lock.unlock();
        }
        if (addre) {
            // 发送消息到kafka 扣减余额
            kafkaTemplate.send(AppVariable.SUB_USECOUNT_TOPIC,uid.toString());
            return   ResultEntity.success(url);
        }
        return ResultEntity.fail("操作失败，请稍后再试！");
    }

    /*
        * @description  获取openai聊天记录
        * @author atchen
        * @date 2024/8/12 15:34
    */
    @PostMapping("/getchatlist")
    public ResultEntity getlist() {
        Long uid = SecurityUtil.getUserDetails().getUid();
        int type = AiTypeEnum.CHAT.getValue();
        int model = AiModelEnum.OPENAI.getValue();
        String cacheKey = AppVariable.getListCacheKey(uid,model,type);  // 缓存key
        Object list = redisTemplate.opsForValue().get(cacheKey);
        if (list==null){
            QueryWrapper<Answer> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("uid",uid);            //查询当前用户的聊天记录
            queryWrapper.eq("model",model);
            queryWrapper.eq("type",type);
            queryWrapper.orderByDesc("aid");// 聚簇索引不需要回表
            List<Answer> answers = iAnswerService.list(queryWrapper);
            // 缓存列表
            redisTemplate.opsForValue().set(cacheKey,answers,1, TimeUnit.DAYS);
            return ResultEntity.success(answers);   //todo
        }
        else {
            return ResultEntity.success(list);
        }
    }
    /*
        * @description 获取绘图的历史信息
        * @author atchen
        * @date 2024/8/12 16:28
    */

    @PostMapping("/getdrawlist")
    public ResultEntity getdrawlist() {
        Long uid = SecurityUtil.getUserDetails().getUid();
        int type = AiTypeEnum.DRAW.getValue();
        int model = AiModelEnum.OPENAI.getValue();
        String cacheKey = AppVariable.getListCacheKey(uid,model,type);  // 缓存key
        Object list = redisTemplate.opsForValue().get(cacheKey);
        if (list==null){
            QueryWrapper<Answer> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("uid",uid);            //查询当前用户的聊天记录
            queryWrapper.eq("model",model);
            queryWrapper.eq("type",type);
            queryWrapper.orderByDesc("aid");// 聚簇索引不需要回表
            List<Answer> answers = iAnswerService.list(queryWrapper);
            // 缓存列表
            redisTemplate.opsForValue().set(cacheKey,answers,1, TimeUnit.DAYS);
            return ResultEntity.success(answers);
        }else {
            return ResultEntity.success(list);
        }
    }

    /*
        * @description 列表缓存key
        * @author atchen
        * @date 2024/8/16
    */


}