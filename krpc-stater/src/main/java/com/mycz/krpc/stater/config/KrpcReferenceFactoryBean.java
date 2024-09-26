package com.mycz.krpc.stater.config;

import com.mycz.krpc.core.annotation.KrpcReference;
import com.mycz.krpc.core.factory.RpcReferenceProxy;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeoutException;

public class KrpcReferenceFactoryBean implements FactoryBean<Object>, InvocationHandler {

    //要代理的对象
    private final Class<?> clazz;

    public KrpcReferenceFactoryBean(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    public boolean isSingleton() {
        return FactoryBean.super.isSingleton();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
        // 获取注解信息
        KrpcReference annotation = null;
        Class<?>[] interfaces = proxy.getClass().getInterfaces();
        for (Class<?> anInterface : interfaces) {
            if (anInterface.isAnnotationPresent(KrpcReference.class)) {
                annotation = anInterface.getAnnotation(KrpcReference.class);
                break;
            }
        }

        String serviceName = annotation == null ? "" : annotation.serviceName();

        Object obj;
        try {
            obj = RpcReferenceProxy.proxy(serviceName, clazz.getName(), method.getName(), method.getParameterTypes(), args);
        } catch (TimeoutException e) {
            throw new Exception("服务调用请求超时");
        } catch (Exception e) {
            throw new Exception("服务调用异常");
        }

        // 代理
        return obj;
    }

    @Override
    public Object getObject() {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{clazz}, this);
    }

    @Override
    public Class<?> getObjectType() {
        return clazz;
    }
}
