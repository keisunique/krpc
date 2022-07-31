package com.mycz.dubboprovider;

import com.mycz.krpc.stater.config.EnableKrpcService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableKrpcService
@SpringBootApplication
public class DubboProviderApplication {

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(DubboProviderApplication.class, args);
    }

}
