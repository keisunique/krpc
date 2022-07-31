package com.mycz.krpc.stater.config;

import com.mycz.krpc.core.KrpcApplication;
import com.mycz.krpc.core.annotation.KrpcReference;
import com.mycz.krpc.core.config.RpcConfig;
import com.mycz.krpc.core.provider.RpcReferenceInvoke;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.annotation.Nonnull;

@Slf4j
public class RpcBeanPostProcessor implements BeanPostProcessor {

    @Autowired
    private RpcProperties rpcProperties;

    /**
     * 扫描到有@KrpcService注解时, 获取配置, 然后启动rpc服务
     */
    @Override
    public Object postProcessBeforeInitialization(Object bean, @Nonnull String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(EnableKrpcService.class)) {
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
        return BeanPostProcessor.super.postProcessBeforeInitialization(bean, beanName);
    }

    /**
     * 扫描到@KrpcReference注解接口的实现类时,保存该类对象到krpc方法调用容器.(服务提供者逻辑)
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, @Nonnull String beanName) throws BeansException {
        Class<?>[] interfaces = bean.getClass().getInterfaces();
        for (Class<?> anInterface : interfaces) {
            if (anInterface.isAnnotationPresent(KrpcReference.class)) {
                RpcReferenceInvoke.addInterface(anInterface, bean);
            }
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
