package com.mycz.krpc.stater.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ConfigurationProperties(prefix = "krpc")
public class RpcProperties {
    // 服务名
    private String name;
    // 主机地址
    private String host;
    // 端口
    private Integer port;
    // 注册中心
    private Registry registry;
    // 配置中心
    private Config config;


    /**
     * 注册中心
     */
    @Data
    @ConfigurationProperties("krpc.registry")
    public static class Registry {
        private Boolean enable;
        private String address;
    }

    /**
     * 配置中心
     */
    @Data
    @ConfigurationProperties("krpc.config")
    public static class Config {
        private Boolean enable;
        private String address;
    }

}
