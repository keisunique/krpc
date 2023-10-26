package com.mycz.krpc.core.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import com.mycz.arch.common.util.RandomKit;
import com.mycz.krpc.core.exception.ServiceRegistryException;
import com.mycz.krpc.core.registry.ServiceRegistry;
import com.mycz.krpc.core.registry.entity.ServiceRegisterResult;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

@Slf4j
public class ConsulServiceRegistry implements ServiceRegistry {

    /**
     * 心跳请求超时时间
     */
    public static final String TIME_OUT = "2s";

    /**
     * 心跳间隔时间
     */
    public static final String INTERVAL = "3s";

    private final ConsulClient client;

    public ConsulServiceRegistry(String address) {
        if (!address.startsWith("consul://")) {
            throw new ServiceRegistryException("consul注册中心地址格式错误");
        }
        String ipPort = address.substring("consul://".length());
        client = new ConsulClient(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]));
    }

    /**
     * 注册服务
     */
    @Override
    public ServiceRegisterResult register(String serviceName, InetSocketAddress address) {
        // 服务基础信息
        NewService service = new NewService();
        String serviceId = serviceName + "." + RandomKit.randomString(RandomKit.CS_LOWERCASE_LETTER + RandomKit.CS_NUMBER, 12);
        service.setId(serviceId);
        service.setName(serviceName);
        service.setAddress(address.getAddress().getHostAddress());
        service.setPort(address.getPort());

        // 心态检查配置
        NewService.Check check = new NewService.Check();
        check.setTcp(address.getHostName() + ":" + address.getPort());
        check.setTimeout(TIME_OUT);
        check.setInterval(INTERVAL);
        service.setCheck(check);

        try {
            client.agentServiceRegister(service);
        } catch (Exception e) {
            log.error("*** 服务注册失败 ***", e);
            System.exit(-1);
        }

        log.info("*** 服务注册成功 - 服务名ID:{}, 地址:{}:{} ", serviceId, address.getHostName(), address.getPort());
        return ServiceRegisterResult.builder().id(serviceId).build();
    }

    /**
     * 解除服务注册
     */
    @Override
    public void deregister(String serviceId) {
        log.info("*** 解除服务注册 - id:{}", serviceId);
        client.agentServiceDeregister(serviceId);
    }
}
