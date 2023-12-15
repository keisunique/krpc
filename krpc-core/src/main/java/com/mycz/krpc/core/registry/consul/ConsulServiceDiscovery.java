package com.mycz.krpc.core.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.catalog.CatalogServiceRequest;
import com.ecwid.consul.v1.catalog.model.CatalogService;
import com.mycz.arch.common.cache.ObjectCache;
import com.mycz.arch.common.util.JsonKit;
import com.mycz.arch.common.util.ListKit;
import com.mycz.krpc.core.registry.ServiceDiscovery;
import com.mycz.krpc.core.registry.entity.ServiceDiscoveryResult;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
        List<ServiceDiscoveryResult> serviceList = new ArrayList<>();
        if (client == null) {
            throw new Exception("ConsulClient未初始化");
        }

        // TODO 这里的缓存可用可不用
        List<CatalogService> value = cache.get(serviceName);
        if (value == null) {
            Response<List<CatalogService>> response = client.getCatalogService(serviceName, CatalogServiceRequest.newBuilder().build());
            value = response.getValue();
            cache.put(serviceName, value, 3);
//            log.info("[ConsulServiceDiscovery][Discovery] - serviceList : {}", JsonKit.toJson(value));
        }

        for (CatalogService service : value) {
            if (service.getServiceName().equals(serviceName)) {
                serviceList.add(ServiceDiscoveryResult.builder()
                        .id(service.getServiceId())
                        .name(service.getServiceName())
                        .address(service.getServiceAddress())
                        .port(service.getServicePort())
                        .build());
            }
        }

        if (ListKit.isEmpty(serviceList)) {
            throw new Exception("无可用服务");
        }

        ServiceDiscoveryResult result = serviceList.get(new Random().nextInt(serviceList.size()));
//        log.info("[ConsulServiceDiscovery][Discovery] - result : {}", JsonKit.toJson(result));

        return result;
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(new Random().nextInt(2));
        }
    }

}
