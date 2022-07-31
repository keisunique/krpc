package com.mycz.krpc.core.registry;

import com.mycz.krpc.core.registry.entity.ServiceRegisterResult;

import java.net.InetSocketAddress;

/**
 * 服务注册
 */
public interface ServiceRegistry {

    /**
     * 注册
     */
    ServiceRegisterResult register(String rpcServiceName, InetSocketAddress inetSocketAddress);

    /**
     * 解除注册
     */
    void deregister(String id);

}
