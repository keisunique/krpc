package com.mycz.krpc.stater.gateway.annotation;

import com.ecwid.consul.v1.ConsulClient;
import com.mycz.arch.common.gateway.MappingEntity;
import com.mycz.arch.common.util.JsonKit;
import com.mycz.krpc.core.annotation.KrpcReference;
import com.mycz.krpc.stater.config.RpcProperties;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Method;
import java.util.Date;

public class MappingBeanPostProcessor implements BeanPostProcessor {

    private final ConsulClient client = new ConsulClient("127.0.0.1", 8500);

    @Autowired
    private RpcProperties rpcProperties;

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
                System.out.println("RequestMappingBeanPostProcessor, beanName = " + beanName);
                ifcc = anInterface;
            }
        }

        if (ifcc != null) {
            Method[] methods = bean.getClass().getMethods();
            for (Method method : methods) {
                RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                if (annotation != null) {
                    String[] paths = annotation.paths();
                    for (String path : paths) {

                        // 获取当前服务名称
                        MappingEntity.Service service = MappingEntity.Service.builder()
                                .name(rpcProperties.getName())
                                .clazz(ifcc.getName())
                                .method(method.getName())
                                .build();

                        MappingEntity entity = MappingEntity.builder()
                                .name(annotation.name())
                                .method(annotation.method())
                                .path(path)
                                .description(annotation.description())
                                .createTime(new Date())
                                .service(service)
                                .build();
                        client.setKVValue(path, JsonKit.toPrettyJson(entity));
                    }
                }
            }
        }

        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
