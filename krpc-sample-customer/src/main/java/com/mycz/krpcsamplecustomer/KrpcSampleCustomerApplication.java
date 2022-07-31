package com.mycz.krpcsamplecustomer;

import com.mycz.krpc.stater.config.EnableKrpcService;
import com.mycz.krpcsampleapi.OrderService;
import com.mycz.krpcsampleapi.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.PostConstruct;

@EnableKrpcService(basePackage = "com.mycz")
@SpringBootApplication
public class KrpcSampleCustomerApplication {

    @Autowired
    private UserService userService;
    @Autowired
    private OrderService orderService;

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(KrpcSampleCustomerApplication.class, args);
    }

    @PostConstruct
    public void test() {
        System.out.println(userService.sayHello());
        System.out.println(userService.sayGoodBye("boy"));

        System.out.println(orderService.total());
    }


}
