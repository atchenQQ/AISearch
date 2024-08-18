package com.atchen.AISearch.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.lang.UUID;
import com.atchen.AISearch.utils.MinioUtil;
import com.atchen.AISearch.utils.NameUtil;
import com.atchen.AISearch.utils.ResultEntity;
import com.atchen.AISearch.utils.SecurityUtil;
import io.minio.errors.*;
import io.reactivex.rxjava3.core.Single;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {

    @Resource
    private MinioUtil minIoUtil;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 生成验证码
     */
    @RequestMapping("/create")
    public ResultEntity createCaptcha(HttpServletRequest request) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        String url = "";
        // 定义图形验证码的长和宽
        LineCaptcha lineCaptcha = CaptchaUtil.createLineCaptcha(120, 40);
        String fileName =   NameUtil.getCaptchaName(request.getRemoteAddr());

        try (InputStream inputStream = new ByteArrayInputStream(lineCaptcha.getImageBytes())) {
            url = minIoUtil.upload(fileName, inputStream, "image/png");
            String code = lineCaptcha.getCode(); // 正确的验证码
            redisTemplate.opsForValue().set(fileName, code,60, TimeUnit.SECONDS); // 验证码存储到 Redis
        }
        return ResultEntity.success(url);
    }

}
