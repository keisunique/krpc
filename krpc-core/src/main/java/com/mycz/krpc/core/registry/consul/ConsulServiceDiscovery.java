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
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ConsulServiceDiscovery implements ServiceDiscovery {

    public ConsulClient client;

    public ConsulServiceDiscovery(String address) {
        String ipPort = address.substring("consul://".length());
        client = new ConsulClient(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]));
    }


    @Override
    public ServiceDiscoveryResult discovery(String serviceName) throws Exception {
        List<ServiceDiscoveryResult> serviceList = new ArrayList<>();
        if (client == null) {
            throw new Exception("ConsulClient未初始化");
        }

        Response<Map<String, Service>> agentServices = client.getAgentServices();
        Map<String, Service> value = agentServices.getValue();
        for (String key : value.keySet()) {
            Service service = value.get(key);
            if (service.getService().equals(serviceName)) {
                serviceList.add(ServiceDiscoveryResult.builder().id(key).name(service.getService()).address(service.getAddress()).port(service.getPort()).build());
            }
        }

        if (ListKit.isEmpty(serviceList)) {
            throw new Exception("服务不可用");
        }

        return serviceList.get(new Random().nextInt(serviceList.size()));
    }

}
