package com.mycz.krpc.core.config;

import lombok.Data;

@Data
public class RpcConfig {
    private String name; // 服务名
    private String host; // 主机ip地址
    private Integer port; // 端口
    private Registry registry; // 注册中心

    /**
     * 注册中心
     */
    @Data
    public static class Registry {
        private Boolean enable; // 是否需要注册服务
        private String address; // 地址
        private String id; // 注册id
        private String serviceIP;
    }

}
