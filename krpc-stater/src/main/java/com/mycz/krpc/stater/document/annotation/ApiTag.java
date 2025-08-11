package com.mycz.krpc.stater.document.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 定义注解的保留策略 - 运行时保留
@Retention(RetentionPolicy.RUNTIME)
// 定义注解可以应用的目标 - 类
@Target({ElementType.TYPE})
public @interface ApiTag {
    String value() default "";
}
