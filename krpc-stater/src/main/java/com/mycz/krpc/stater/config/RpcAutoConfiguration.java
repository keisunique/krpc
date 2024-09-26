package com.mycz.krpc.stater.config;

import com.ecwid.consul.v1.ConsulClient;
import com.mycz.arch.common.util.JsonKit;
import com.mycz.arch.common.util.StringKit;
import com.mycz.krpc.core.KrpcApplication;
import com.mycz.krpc.core.annotation.KrpcReference;
import com.mycz.krpc.core.config.RpcConfig;
import com.mycz.krpc.core.provider.RpcReferenceInvoke;
import com.mycz.krpc.stater.gateway.annotation.RequestMapping;
import com.mycz.krpc.stater.gateway.annotation.RequestMappings;
import com.mycz.krpc.stater.gateway.entity.MappingEntity;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@EnableConfigurationProperties({RpcProperties.class, RpcProperties.Registry.class, RpcProperties.Config.class})
@Configuration(proxyBeanMethods = false)
public class RpcAutoConfiguration {

    @Autowired
    private RpcProperties rpcProperties;

    private ConsulClient client;

    @EventListener
    public void handleRequestMapping(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();


    }

    @EventListener
    public void handleContextRefreshed(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();

        String[] beanNamesForAnnotation = applicationContext.getBeanNamesForAnnotation(EnableKrpcService.class);
        if (beanNamesForAnnotation.length == 0) {
            return;
        }

        int count = 0;

        String ipPort = rpcProperties.getRegistry().getAddress().substring("consul://".length());
        client = new ConsulClient(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]));

        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            for (Class<?> iface : interfaces) {
                if (iface.isAnnotationPresent(KrpcReference.class)) {
                    if (Proxy.isProxyClass(bean.getClass())) {
                        continue;
                    }
                    RpcReferenceInvoke.addInterface(iface, bean);

                    Method[] declaredMethods = bean.getClass().getDeclaredMethods();
                    for (Method method : declaredMethods) {
                        RequestMapping annotations = method.getAnnotation(RequestMapping.class);
                        if (annotations != null) {
                            this.processMapper(annotations, iface.getName(), method);
                            count++;
                        }

                        RequestMappings annotationsM = method.getAnnotation(RequestMappings.class);
                        if (annotationsM != null) {
                            for (RequestMapping requestMapping : annotationsM.value()) {
                                this.processMapper(requestMapping, iface.getName(), method);
                                count++;
                            }
                        }
                    }
                }
            }
        }

        log.info("RequestMapping接口登记数量: {}", count);

//
//        if (RpcReferenceInvoke.getInterfaceSize() == 0) {
//            log.warn("未实现任何RPC接口, 不启动Rpc服务");
//            return;
//        }

        // 启动RPC服务
        RpcConfig config = new RpcConfig();
        config.setName(rpcProperties.getName());
        config.setHost(rpcProperties.getHost());
        config.setPort(rpcProperties.getPort());

        RpcConfig.Registry registry = new RpcConfig.Registry();
        registry.setEnable(rpcProperties.getRegistry().getEnable());
        registry.setAddress(rpcProperties.getRegistry().getAddress());
        config.setRegistry(registry);

        new KrpcApplication(config).start();
    }

    private void processMapper(RequestMapping mapping, String clazz, Method method) {
        if (mapping != null) {
            if (StringKit.isBlank(mapping.path())) {
                return;
            }

            String path = mapping.path();
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            // 构造映射路径信息
            MappingEntity entity = MappingEntity.builder()
                    .name(mapping.name())
                    .method(mapping.method())
                    .path(path)
                    .authority(mapping.authority())
                    .description(mapping.description())
                    .responseType(mapping.responseType())
                    .deliverPayload(mapping.deliverPayload())
                    .deliverParams(mapping.deliverParams())
                    .createTime(new Date())
                    .service(MappingEntity.Service.builder()
                            .name(rpcProperties.getName())
                            .clazz(clazz)
                            .method(method.getName())
                            .paramType(method.getParameterCount() > 0 ? method.getParameterTypes()[0].getName() : "")
                            .build())
                    .build();

            // 写入consul
            if (path.startsWith("/")) {
                path = path.replaceFirst("/", "");
            }
            client.setKVValue(path + "/" + mapping.method(), JsonKit.toPrettyJson(entity));
        }
    }

}
