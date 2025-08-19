package com.mycz.krpc.core.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.ecwid.consul.v1.health.model.HealthService;
import com.mycz.arch.common.cache.ObjectCache;
import com.mycz.arch.common.util.JsonKit;
import com.mycz.arch.common.util.ListKit;
import com.mycz.krpc.core.registry.ServiceDiscovery;
import com.mycz.krpc.core.registry.entity.ServiceDiscoveryResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class ConsulServiceDiscovery implements ServiceDiscovery {

    public ConsulClient client;

    private final ObjectCache<List<CatalogService>> cache = new ObjectCache();

    public ConsulServiceDiscovery(String address) {
        String ipPort = address.substring("consul://".length());
        client = new ConsulClient(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]));
    }
    
    @Override
    public ServiceDiscoveryResult discovery(String serviceName) throws Exception {
        if (client == null) {
            throw new Exception("ConsulClient未初始化");
        }

        List<CatalogService> services = cache.get(serviceName);
        if (services == null) {
            Response<List<CatalogService>> response = client.getCatalogService(serviceName, CatalogServiceRequest.newBuilder().build());
            services = response.getValue();
            if (services == null) {
                services = new ArrayList<>();
            }
            cache.put(serviceName, services, 3);
        } else if (services.isEmpty()) {
            throw new Exception("无可用服务: " + serviceName);
        }

        // 过滤匹配的服务
        List<ServiceDiscoveryResult> matchedServices = services.stream()
                .filter(s -> serviceName.equals(s.getServiceName()))
                .map(s -> ServiceDiscoveryResult.builder()
                        .id(s.getServiceId())
                        .name(s.getServiceName())
                        .address(s.getServiceAddress())
                        .port(s.getServicePort())
                        .build())
                .toList();

        if (matchedServices.isEmpty()) {
            throw new Exception("无可用服务: " + serviceName);
        }

        return matchedServices.get(ThreadLocalRandom.current().nextInt(matchedServices.size()));
    }

}
