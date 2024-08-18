package com.atchen.AISearch.filter;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.atchen.AISearch.entity.SecurityUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-11
 * @Description: 登录认证的过滤器 在这里可以做一些登录认证的操作，比如检查用户是否登录，是否有权限访问等等 userDetails
 * @Version: 1.0
 */
@Configuration
public class LoginAuthenticatioFilter extends OncePerRequestFilter {
    @Value("${jwt.secret}")
    private String secret;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 获取 jwt
        String token = request.getHeader("Authrization");
        // 判空
        if (StringUtils.hasLength(token)){
            // 校验jwt有效性
            if (JWTUtil.verify(token,secret.getBytes())) {
                // 校验通过,获取用户信息
                JWT jwt = JWTUtil.parseToken(token);
                if (jwt!= null&&jwt.getPayload("uid")!= null){
                    Long uid  = Long.parseLong(jwt.getPayload("uid").toString());
                    String username = (String) jwt.getPayload("username");
                    // 创建用户对象
                    SecurityUserDetails userDetails = new SecurityUserDetails(uid,username,"");
                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
    