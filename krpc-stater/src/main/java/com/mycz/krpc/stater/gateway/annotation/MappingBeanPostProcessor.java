package com.mycz.krpc.stater.gateway.annotation;

import com.ecwid.consul.v1.ConsulClient;
import com.mycz.arch.common.gateway.MappingEntity;
import com.mycz.arch.common.util.JsonKit;
import com.mycz.arch.common.util.StringKit;
import com.mycz.krpc.core.annotation.KrpcReference;
import com.mycz.krpc.stater.config.RpcProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Date;

public class MappingBeanPostProcessor implements BeanPostProcessor {

    private ConsulClient client;

    @Autowired
    private RpcProperties rpcProperties;

    @PostConstruct
    public void init() {
        String ipPort = rpcProperties.getRegistry().getAddress().substring("consul://".length());
        client = new ConsulClient(ipPort.split(":")[0], Integer.parseInt(ipPort.split(":")[1]));
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {

        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {

        Class<?>[] interfaces = bean.getClass().getInterfaces();
        Class<?> ifcc = null;
        for (Class<?> anInterface : interfaces) {
            if (anInterface.isAnnotationPresent(KrpcReference.class)) {
                ifcc = anInterface;
            }
        }

        if (ifcc != null) {
            Method[] methods = bean.getClass().getMethods();
            for (Method method : methods) {
                RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                if (annotation != null) {
                    if (StringKit.isBlank(annotation.path())) {
                        continue;
                    }

                    // 构造映射路径信息
                    MappingEntity entity = MappingEntity.builder()
                            .name(annotation.name())
                            .method(annotation.method())
                            .path(annotation.prefix().getPrefix() + annotation.path())
                            .description(annotation.description())
                            .createTime(new Date())
                            .service(MappingEntity.Service.builder()
                                    .name(rpcProperties.getName())
                                    .clazz(ifcc.getName())
                                    .method(method.getName())
                                    .paramType(method.getParameterCount() > 0 ? method.getParameterTypes()[0].getName() : "")
                                    .build())
                            .build();

                    // 写入consul
                    client.setKVValue("/" + annotation.prefix().getPrefix() + annotation.path() + "/" + annotation.method(), JsonKit.toPrettyJson(entity));
                }
            }
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
