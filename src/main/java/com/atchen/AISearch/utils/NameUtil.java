package com.atchen.AISearch.utils;

import cn.hutool.crypto.SecureUtil;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-10
 * @Description: 拼接key的工具类
 * @Version: 1.0
 */


public class NameUtil {

    /*
        * @description  获取验证码的key
        * @author atchen
        * @date 2024/8/10 21:14
    */
    public  static String getCaptchaName(String name){
        return   "captcha-"+ SecureUtil.md5(name);
    }
}
    