package com.atchen.AISearch.config;
import com.atchen.AISearch.filter.LoginAuthenticatioFilter;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * @Author: atchen
 * @CreateTime: 2024-08-11
 * @Description: 安全框架配置
 * @Version: 1.0
 */

@Configuration
@EnableWebSecurity  // 启用安全框架
public class SecurityConfig {
    @Resource
    private LoginAuthenticatioFilter   loginAuthenticatioFilter;
    @Bean
    public PasswordEncoder passwordEncoder() {
        // 使用BCrypt加密算法
        return new BCryptPasswordEncoder();
    }

    // 配置安全过滤链
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return    http
                 .httpBasic(AbstractHttpConfigurer::disable)  // 禁用明文验证
                 .csrf(AbstractHttpConfigurer::disable)  // 禁用CSRF验证
                 .formLogin(AbstractHttpConfigurer::disable) // 禁用默认登录
                .headers(AbstractHttpConfigurer::disable)   // 禁用默认安全头 支持iframe访问
                 .logout(AbstractHttpConfigurer::disable) // 禁用默认注销
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 禁用会话管理
                .authorizeHttpRequests(auth ->{
                    auth.requestMatchers(
                            // 允许访问的资源
                            "/js/**",
                            "/css/**",
                            "/layui/**",
                            "/login.html",
                            "/index.html",
                            "/register.html",
                            "/user/login",
                            "/user/register",
                            "/captcha/create",
                            "/swagger-ui/**",
                            "/v3/**",
                            "/doc.html",
                            "/webjars/**",
                            "/"

                    ).permitAll()
                    .anyRequest().authenticated();  // 其他请求需要认证
                })
                // 添加自定义登录过滤器
                .addFilterBefore(loginAuthenticatioFilter, UsernamePasswordAuthenticationFilter.class)
                .build(); // 开启HTTP Basic认证
    }
}
    