package com.mycz.krpc.core.config;

import lombok.Data;

@Data
public class RpcConfig {

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
    public static class Registry {
        // 是否需要注册服务
        private Boolean enable;
        // 地址
        private String address;
        // 注册id
        private String id;
    }

    /**
     * 配置中心
     */
    @Data
    public static class Config {
        // 是否需要接入配置中心
        private Boolean enable;
        // 地址
        private String address;
    }


}
