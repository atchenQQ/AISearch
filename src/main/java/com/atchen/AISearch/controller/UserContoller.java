package com.atchen.AISearch.controller;
import cn.hutool.jwt.JWTUtil;
import com.atchen.AISearch.entity.User;
import com.atchen.AISearch.entity.dto.UserDTO;
import com.atchen.AISearch.service.IUserService;
import com.atchen.AISearch.utils.NameUtil;
import com.atchen.AISearch.utils.ResultEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-10
 * @Description: 用户表单验证
 * @Version: 1.0
 */

@RestController
@RequestMapping("/user")
@Tag(name = "用户控制器")
public class UserContoller {
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private IUserService iUserService;
    @Value("${jwt.secret}")
    private String jwtSecret;
    @Resource
    private PasswordEncoder passwordEncoder;
    /*
        * @description 登录方法
        * @author atchen
        * @date 2024/8/11
    */
    @PostMapping("/login")
    @Operation(summary = "登录接口")
    @Parameters({
            @Parameter(name = "username", description = "用户名", required = true),
            @Parameter(name = "password", description = "密码", required = true),
            @Parameter(name = "captcha", description = "验证码", required = true)
    })
    public ResultEntity login(@Validated UserDTO userDTO, HttpServletRequest request) {
          // 校验验证码
          String redisCaptchaKey  = NameUtil.getCaptchaName(request.getRemoteAddr());
          String redisCaptcha = (String) redisTemplate.opsForValue().get(redisCaptchaKey);
          if (!StringUtils.hasLength(redisCaptcha)||!userDTO.getCaptcha().equalsIgnoreCase(redisCaptcha)){
              return ResultEntity.fail("验证码错误！");
          }
          // 校验用户名和密码
          User user = iUserService.lambdaQuery().eq(User::getUsername, userDTO.getUsername()).one();
          if (user !=null&& passwordEncoder.matches(userDTO.getPassword(),user.getPassword())){
              // 生成JWT
              HashMap<String, Object> map = new HashMap<>();
              map.put("uid",user.getUid());
              map.put("username",user.getUsername());
              HashMap<String,String> result = new HashMap<>();
              result.put("jwt",JWTUtil.createToken(map, jwtSecret.getBytes()));
              result.put("username",user.getUsername());
              return ResultEntity.success(result);
          }
        return ResultEntity.fail("用户名或密码错误");
    }

    /*
        * @description  注册方法
        * @author atchen
        * @date 2024/8/10
    */
    @PostMapping("/register")
    @Operation(summary = "注册接口")
    @Parameters({
            @Parameter(name = "username", description = "用户名", required = true),
            @Parameter(name = "password", description = "密码", required = true)
    })
    public ResultEntity register(@Validated User user) {
        // 密码加盐处理
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        boolean result = iUserService.save(user);
        if (!result){
            return ResultEntity.fail("未知错误！");
        }
        return  ResultEntity.success(result);
    }
}
    