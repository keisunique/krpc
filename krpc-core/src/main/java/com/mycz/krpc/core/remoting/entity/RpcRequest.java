package com.mycz.krpc.core.remoting.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcRequest implements Serializable {
    // 请求id
    private String requestId;
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

    @JsonIgnore
    public String getRpcServiceName() {
        return this.getInterfaceName() + this.getGroup() + this.getVersion();
    }

}
