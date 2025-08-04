package com.mycz.krpc.stater.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

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
    // vault
    private Vault vault;
    // document
    private Document document;


    /**
     * 注册中心
     */
    @Data
    @ConfigurationProperties("krpc.registry")
    public static class Registry {
        private Boolean enable;
        private String address; // 注册中心的地址
        private String serviceIP; // 服务访问地址, 如需公网访问时可配置此项
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

    /**
     * Vault配置中心
     */
    @Data
    @ConfigurationProperties("krpc.vault")
    public static class Vault {
        private Boolean enable = false;
        private String url;
        private String token;
        private String[] secretPath = {"secret/data/krpc"};
    }

    /**
     * api文档生成
     */
    @Data
    @ConfigurationProperties("krpc.document")
    public static class Document {
        private Boolean enable = false;
        private String url;
    }

}
