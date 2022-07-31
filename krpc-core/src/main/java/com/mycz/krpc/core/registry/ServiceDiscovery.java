package com.mycz.krpc.core.registry;

import com.mycz.krpc.core.registry.entity.ServiceDiscoveryResult;
import com.mycz.krpc.core.remoting.entity.RpcRequest;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 服务发现
 */
public interface ServiceDiscovery {

    ServiceDiscoveryResult discovery(String serviceName);

}
