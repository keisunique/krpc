package com.mycz.krpc.core.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface KrpcReference {

    String serviceName() default "";

}
