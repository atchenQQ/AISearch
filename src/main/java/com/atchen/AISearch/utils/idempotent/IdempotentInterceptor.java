package com.atchen.AISearch.utils.idempotent;
import cn.hutool.crypto.SecureUtil;
import com.atchen.AISearch.entity.SecurityUserDetails;
import com.atchen.AISearch.utils.SecurityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-15
 * @Description:
 * @Version: 1.0
 */

@Component
public class IdempotentInterceptor implements HandlerInterceptor {
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private RedisTemplate redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Idempotent idempotent  =  null;
        try {
            idempotent =  ( (HandlerMethod)handler).getMethod().getAnnotation(Idempotent.class);
        }catch (Exception e){}
        if(idempotent!= null){
            String id =  createId(request);
            if(redisTemplate.opsForValue().get(id)!= null){
                return false;
            }
            else {
                redisTemplate.opsForValue().set(id, "true", idempotent.time(),
                        TimeUnit.SECONDS);
                return  true;
            }
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }


    // 生成幂等唯一id  用户id+请求参数 md5加密
    private String createId(HttpServletRequest request) throws JsonProcessingException {
        Long uid = 0L;
        SecurityUserDetails userDetails = SecurityUtil.getUserDetails();
        if (userDetails!= null){
            uid = userDetails.getUid();
        }
        String value = objectMapper.writeValueAsString(request.getParameterMap());
        return SecureUtil.md5(uid + value);
    }
}
    