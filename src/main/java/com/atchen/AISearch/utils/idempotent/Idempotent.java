package com.atchen.AISearch.utils.idempotent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
    * @description 自定义幂等注解
    * @author atchen
    * @date 2024/8/15
*/
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    // 幂等的过期时间，单位秒，默认60秒
    long time() default 60;
}
