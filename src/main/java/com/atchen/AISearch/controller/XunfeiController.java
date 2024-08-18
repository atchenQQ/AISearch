package com.atchen.AISearch.controller;

import cn.hutool.http.HttpRequest;
import com.atchen.AISearch.entity.Answer;
import com.atchen.AISearch.entity.enums.AiModelEnum;
import com.atchen.AISearch.entity.enums.AiTypeEnum;
import com.atchen.AISearch.service.IAnswerService;
import com.atchen.AISearch.utils.AppVariable;
import com.atchen.AISearch.utils.MinioUtil;
import com.atchen.AISearch.utils.ResultEntity;


import com.atchen.AISearch.utils.SecurityUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import okhttp3.HttpUrl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-12
 * @Description: 讯飞大模型接入
 * @Version: 1.0
 */


@RestController
@RequestMapping("/xunfei")
public class XunfeiController {
    @Value("${xunfei.chat.url}")
    private String chatUrl;
    @Value("${xunfei.chat.api-key}")
    private String chatApiKey;
    @Value("${xunfei.chat.api-secret}")
    private String chatApiSecret;

    @Value("${xunfei.draw.app-id}")
    private String appId;
    @Value("${xunfei.draw.api-key}")
    private String drawApiKey;
    @Value("${xunfei.draw.api-secret}")
    private String drawApiSecret;
    @Value("${xunfei.draw.host-url}")
    private String hostUrl;
    @Resource
    private ObjectMapper objectMapper;   // jackson 解析json 的工具类
    @Resource
    private IAnswerService answerService;   // 答案服务接口
    @Resource
    private MinioUtil minioUtil;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private KafkaTemplate kafkaTemplate;
    @Resource
    private RedisTemplate   redisTemplate;

    /*
        * @description  调用对话功能
        * @author atchen
        * @date 2024/8/12
    */
    @PostMapping("/chat")
    public ResultEntity chat(String question) throws JsonProcessingException {
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        String content = "";  // 最终返回的结果
        boolean saveResult = false;  // 保存答案结果
        // 分布式锁
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.XUNFEI.getValue(), AiTypeEnum.CHAT.getValue());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(30, TimeUnit.SECONDS);
            if (!tryLock){
                return ResultEntity.fail("请求过于频繁，请稍后再试！");
            }
            // 业务逻辑
            String bodyJson = "{\n" +
                    "    \"model\":\"generalv3.5\",\n" +
                    "    \"messages\": [\n" +
                    "        {\n" +
                    "            \"role\": \"user\",\n" +
                    "            \"content\": \"" + question + "\"\n" +
                    "        }\n" +
                    "    ]\n" +
                    "}";
            String result = HttpRequest.post(chatUrl)     // 一个很大的json对象  用jsonview插件可以查看
                    .header("Content-Type", "application/json")
                    .header("Authorization","Bearer "+chatApiKey+":"+chatApiSecret)
                    .body(bodyJson)
                    .execute().body();
           // 解析返回结果
            HashMap<String, Object> resultMap = objectMapper.readValue(result, HashMap.class);
            if(!resultMap.get("code").toString().equals("0")){
                // 调用失败
                return ResultEntity.fail(resultMap.get("message").toString());
            }
            ArrayList choices = (ArrayList) resultMap.get("choices"); //
            LinkedHashMap<String, Object> choicesMap = (LinkedHashMap) choices.get(0);   // message  linkedHashMap<String, Object>
            LinkedHashMap<String, Object> message = (LinkedHashMap<String, Object>) choicesMap.get("message");   // content  linkedHashMap<String, Object>
            content = message.get("content").toString();   // 最终返回的结果
            Answer answer = new Answer();
            answer.setContent(content);
            answer.setUid(SecurityUtil.getUserDetails().getUid());
            answer.setTitle(question);
            answer.setType(AiTypeEnum.CHAT.getValue());
            answer.setModel(AiModelEnum.XUNFEI.getValue());
            saveResult = answerService.save(answer);
        }catch (Exception e){}
        finally {
            lock.unlock();
        }
        if (saveResult) {
            // 发送消息到kafka 扣减余额
            kafkaTemplate.send(AppVariable.SUB_USECOUNT_TOPIC,uid.toString());
            return ResultEntity.success(content);
        }
        return ResultEntity.fail("操作失败，请稍后再试！");
    }

    @PostMapping("/draw")
    public ResultEntity draw(String question) throws Exception {
        if (!StringUtils.hasLength(question)) {
            //输入为空
            return ResultEntity.fail("请先输入内容！");
        }
        String imgUrl = "";  // 最终返回的图片地址
        boolean saveResult = false;  // 保存答案结果
        // 分布式锁
        Long uid = SecurityUtil.getUserDetails().getUid();
        String lockKey = AppVariable.getModelLockKeY(uid,
                AiModelEnum.XUNFEI.getValue(), AiTypeEnum.DRAW.getValue());
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean tryLock = lock.tryLock(30, TimeUnit.SECONDS);
            if (!tryLock){
                return ResultEntity.fail("请求过于频繁，请稍后再试！");
            }
            // 业务逻辑
            String authUrl = getAuthUrl(hostUrl, drawApiKey, drawApiSecret);
            String json = "{\n" +
                    "  \"header\": {\n" +
                    "    \"app_id\": \"" + appId + "\"\n" +
                    "    },\n" +
                    "  \"parameter\": {\n" +
                    "    \"chat\": {\n" +
                    "      \"domain\": \"general\",\n" +
                    "      \"width\": 512,\n" +
                    "      \"height\": 512\n" +
                    "      }\n" +
                    "    },\n" +
                    "  \"payload\": {\n" +
                    "    \"message\": {\n" +
                    "      \"text\": [\n" +
                    "        {\n" +
                    "          \"role\": \"user\",\n" +
                    "          \"content\": \"" + question + "\"\n" +
                    "        }\n" +
                    "      ]\n" +
                    "    }\n" +
                    "  }\n" +
                    "}";
            String result =  HttpRequest.post(authUrl).body(json).execute().body();
            HashMap<String, Object> resultMap = objectMapper.readValue(result, HashMap.class);
            LinkedHashMap<String, Object> payload = (LinkedHashMap<String, Object>) resultMap.get("payload");
            LinkedHashMap<String, Object> choices = (LinkedHashMap<String, Object>) payload.get("choices");
            ArrayList<LinkedHashMap<String, Object>> text = (ArrayList<LinkedHashMap<String, Object>>)
                    choices.get("text");
            LinkedHashMap<String, Object> contentMap =  text.get(0);
            String content = contentMap.get("content").toString();
           // base64 数据流上传到minio
            try(ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(content))){
                  String fileName =  "xf-draw-"
                        +UUID.randomUUID().toString().replace("-", "");
                  imgUrl  =  minioUtil.upload(fileName, inputStream,"image/png");
            }
            // 保存到数据库
            Answer answer = new Answer();
            answer.setContent(imgUrl);
            answer.setUid(SecurityUtil.getUserDetails().getUid());
            answer.setTitle(question);
            answer.setType(AiTypeEnum.DRAW.getValue());
            answer.setModel(AiModelEnum.XUNFEI.getValue());
            saveResult = answerService.save(answer);
        }catch (Exception e){}
        finally {
            lock.unlock();
        }
        if (saveResult){
            // 发送消息到kafka 扣减余额
            kafkaTemplate.send(AppVariable.SUB_USECOUNT_TOPIC,uid.toString());
            return ResultEntity.success(imgUrl);
        }
        return ResultEntity.fail("操作失败，请稍后再试！");
    }

    /*
        * @description   调用讯飞大模型历史对话信息
        * @author atchen
        * @date 2024/8/13
    */
    @PostMapping("/getchatlist")
    public ResultEntity getChatList() {
        Long uid = SecurityUtil.getUserDetails().getUid();
        int type =AiTypeEnum.CHAT.getValue();
        int model =AiModelEnum.XUNFEI.getValue();
        String cacheKey = AppVariable.getListCacheKey(uid,model,type);  // 缓存key
        Object list = redisTemplate.opsForValue().get(cacheKey);
        if (list== null){
            QueryWrapper<Answer> queryWrapper = new QueryWrapper();
            queryWrapper.eq("uid", uid);
            queryWrapper.eq("type",type );
            queryWrapper.eq("model", model);
            queryWrapper.orderByDesc("aid");
            List<Answer> answers = answerService.list(queryWrapper);
            redisTemplate.opsForValue().set(cacheKey,answers,1, TimeUnit.DAYS);
            return ResultEntity.success(answers);
        }
        return ResultEntity.success(list);
    }

    /*
        * @description 获取所有绘图信息
        * @author atchen
        * @date 2024/8/13
    */
     @PostMapping("/getdrawlist")
     public ResultEntity getDrawList() {
         Long uid = SecurityUtil.getUserDetails().getUid();
         int type =AiTypeEnum.DRAW.getValue();
         int model =AiModelEnum.XUNFEI.getValue();
         String cacheKey = AppVariable.getListCacheKey(uid,model,type);  // 缓存key
         Object list = redisTemplate.opsForValue().get(cacheKey);
         if (list== null){
             QueryWrapper queryWrapper = new QueryWrapper();
             queryWrapper.eq("uid", uid);
             queryWrapper.eq("type", type);
             queryWrapper.eq("model", model);
             queryWrapper.orderByDesc("aid");
             List answers = answerService.list(queryWrapper);
             redisTemplate.opsForValue().set(cacheKey,answers,1, TimeUnit.DAYS);
             return ResultEntity.success(answers);
         }
         return ResultEntity.success(list);
     }


    /*
        * @description   讯飞大模型签名方法
        * @author atchen
        * @date 2024/8/13
    */
    public static String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        // date="Thu, 12 Oct 2023 03:05:28 GMT";
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" + "date: " + date + "\n" + "POST " + url.getPath() + " HTTP/1.1";
        // System.err.println(preStr);
        // SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);

        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        // Base64加密
        String sha = Base64.getEncoder().encodeToString(hexDigits);
        // System.err.println(sha);
        // 拼接
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        // 拼接地址
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder().//
                addQueryParameter("authorization", Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();
        return httpUrl.toString();
    }
}
    