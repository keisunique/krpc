package com.mycz.krpc.core.remoting.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {
    // 请求源ip
    private String ip;
    // 请求id
    private String traceId;
    // 服务名称
    private String serviceName;
    // 接口名称
    private String interfaceName;
    // 方法名称
    private String methodName;
    // 参数类型
    private Class<?>[] paramTypes;
    // 参数值
    private Object[] parameters;
    // 接口版本
    private String version;
    // 接口组
    private String group;
    // 请求上下文
    private Map<String, Object> context;

}
