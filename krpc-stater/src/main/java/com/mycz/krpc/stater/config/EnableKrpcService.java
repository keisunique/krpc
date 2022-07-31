package com.mycz.krpc.stater.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(RpcReferenceRegistrar.class)
@Inherited
@Documented
public @interface EnableKrpcService {

    String[] basePackage() default {};
}
