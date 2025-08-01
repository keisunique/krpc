package com.mycz.krpc.stater.gateway.annotation;

import com.mycz.arch.common.gateway.RequestMethod;
import com.mycz.krpc.stater.config.RpcReferenceRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(RpcReferenceRegistrar.class)
@Repeatable(RequestMappings.class)
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
    String path() default "";

    /**
     * 请求路径前缀
     */
    String prefix() default "";

    /**
     * 是否检查访问权限
     */
    boolean authority() default true;

    /**
     * 权限类型
     */
    AuthorityType authorityType() default AuthorityType.BEARER_TOKEN;

    /**
     * 方法描述
     */
    String description() default "";

    /**
     * 响应类型
     */
    ResponseType responseType() default ResponseType.API;

    /**
     * 是否传递原始http请求体
     */
    boolean deliverPayload() default false;

    /**
     * 是否床底原始请求参数
     */
    boolean deliverParams() default false;

}
