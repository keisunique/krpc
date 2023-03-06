package com.mycz.krpc.stater.config;

import com.mycz.krpc.stater.gateway.MappingBeanPostProcessor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({RpcProperties.class, RpcProperties.Registry.class, RpcProperties.Config.class})
@Configuration(proxyBeanMethods = false)
public class RpcAutoConfiguration {

    private final RpcProperties rpcProperties;

    public RpcAutoConfiguration(RpcProperties rpcProperties) {
        this.rpcProperties = rpcProperties;
    }

    @Bean
    public RpcBeanPostProcessor rpcStarter() {
        return new RpcBeanPostProcessor();
    }

    @Bean
    public MappingBeanPostProcessor mappingBeanPostProcessor() {
        return new MappingBeanPostProcessor();
    }

}
