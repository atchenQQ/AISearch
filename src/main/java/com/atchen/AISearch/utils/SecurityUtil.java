package com.atchen.AISearch.utils;

import com.atchen.AISearch.entity.SecurityUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-11
 * @Description: 获取Security上下文用户信息
 * @Version: 1.0
 */


public class SecurityUtil {
    // 获取当前登录用户信息
    public static SecurityUserDetails getUserDetails() {
        SecurityUserDetails userDetails = null;
        try {
            userDetails = (SecurityUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (Exception e) {}
        return userDetails;
    }
}
    