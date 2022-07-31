package com.mycz.krpc.core.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.Service;
import com.esotericsoftware.minlog.Log;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.mycz.arch.common.util.ListKit;
import com.mycz.krpc.core.config.RpcConfig;
import com.mycz.krpc.core.factory.ApplicationContext;
import com.mycz.krpc.core.registry.ServiceDiscovery;
import com.mycz.krpc.core.registry.entity.ServiceDiscoveryResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ConsulServiceDiscovery implements ServiceDiscovery {

    /**
     * 服务信息缓存
     */
    private final Cache<String, List<ServiceDiscoveryResult>> serviceCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(30, TimeUnit.SECONDS)
            .build();

    public ConsulClient client = null;

    public ConsulServiceDiscovery() {
        RpcConfig rpcConfig = ApplicationContext.getInstance(RpcConfig.class);
        if (rpcConfig.getRegistry() != null) {
            String address = rpcConfig.getRegistry().getAddress();
            this.initConsulClient(address);
        }
    }

    public ConsulServiceDiscovery(String address) {
        this.initConsulClient(address);
    }

    public void initConsulClient(String address) {
        String ipPort = address.substring("consul://".length());
        client = new ConsulClient(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]));
    }

    @Override
    public ServiceDiscoveryResult discovery(String serviceName) {
        List<ServiceDiscoveryResult> serviceList = serviceCache.getIfPresent(serviceName);
        if (ListKit.isEmpty(serviceList)) {
            serviceList = new ArrayList<>();
            if (client != null) {
                Response<Map<String, Service>> agentServices = client.getAgentServices();
                Map<String, Service> value = agentServices.getValue();
                for (String key : value.keySet()) {
                    Service service = value.get(key);
                    if (service.getService().equals(serviceName)) {
                        serviceList.add(ServiceDiscoveryResult.builder().id(key).name(service.getService()).address(service.getAddress()).port(service.getPort()).build());
                    }
                }
                serviceCache.put(serviceName, serviceList);
            } else {
                Log.warn("consul client 没有初始化!");
            }
        }

        return serviceList.get(new Random().nextInt(serviceList.size()));
    }

}
