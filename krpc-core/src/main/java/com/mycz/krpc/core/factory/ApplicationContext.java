package com.mycz.krpc.core.factory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用全局容器
 */
public class ApplicationContext {

    private static final ConcurrentHashMap<Class<?>, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    /**
     * 获取实例对象
     */
    public static <T> T getInstance(Class<T> tClass) {
        return tClass.cast(OBJECT_MAP.get(tClass));
    }

    public static void addInstance(Class<?> tclass, Object obj) {
        OBJECT_MAP.put(tclass, obj);
    }

}
