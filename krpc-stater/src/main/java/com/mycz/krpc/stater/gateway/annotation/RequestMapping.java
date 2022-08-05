package com.mycz.krpc.stater.gateway.annotation;

import com.mycz.krpc.stater.config.RpcReferenceRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(RpcReferenceRegistrar.class)
@Inherited
@Documented
public @interface RequestMapping {

    /**
     * 映射名称
     */
    String name() default "";

    /**
     * 请求方法
     */
    RequestMethod method() default RequestMethod.GET;

    /**
     * 请求路径
     */
    String[] paths() default {};

    /**
     * 方法描述
     */
    String description() default "";

}
