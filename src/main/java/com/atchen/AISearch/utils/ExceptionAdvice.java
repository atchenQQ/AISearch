package com.atchen.AISearch.utils;

import com.atchen.AISearch.utils.ResultEntity;


import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Author: atchen
 * @CreateTime: 2024-08-10
 * @Description: 全局异常处理
 * @Version: 1.0
 */

@RestControllerAdvice
public class ExceptionAdvice {
    // 绑定参数异常
    @ExceptionHandler(BindException.class)
    public ResultEntity handleBindException(BindException e) {
        // 处理参数绑定异常
        return ResultEntity.fail(e.getBindingResult().getFieldErrors().get(0).getDefaultMessage());
    }
    // 处理其他异常
    @ExceptionHandler(Exception.class)
    public ResultEntity handleException(Exception e) {
        return ResultEntity.fail(e.getMessage());
    }
}
    