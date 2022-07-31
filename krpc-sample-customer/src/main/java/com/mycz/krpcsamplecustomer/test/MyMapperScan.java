package com.mycz.krpcsamplecustomer.test;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(MyMapperRegistrar.class)
@Inherited
@Documented
public @interface MyMapperScan {

    String[] value();

}
