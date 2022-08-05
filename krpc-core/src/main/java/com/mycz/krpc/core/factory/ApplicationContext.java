package com.mycz.krpc.core.factory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用实例容器 + 全局上下文
 */
public class ApplicationContext {

    // rpc运行实例容器
    private static final ConcurrentHashMap<Class<?>, Object> OBJECT_MAP = new ConcurrentHashMap<>();

    public static <T> T getInstance(Class<T> tClass) {
        return tClass.cast(OBJECT_MAP.get(tClass));
    }

    public static void addInstance(Class<?> tclass, Object obj) {
        OBJECT_MAP.put(tclass, obj);
    }

    // 上下文容器
    private static final ThreadLocal<RpcContext> CONTEXT = ThreadLocal.withInitial(() -> RpcContext.builder()
            .ip("")
            .requestId("")
            .context(new HashMap<>())
            .build());


    public static String getIp() {
        return CONTEXT.get().getIp();
    }

    public static void setIp(String ip) {
        CONTEXT.get().setIp(ip);
    }

    public static String getRequestId() {
        return CONTEXT.get().getRequestId();
    }

    public static void setRequestId(String requestId) {
        CONTEXT.get().setRequestId(requestId);
    }

    public static void addAttribute(String key, Object value) {
        CONTEXT.get().getContext().put(key, value);
    }

    public static Object getAttribute(String key) {
        return CONTEXT.get().getContext().get(key);
    }

    public static void addAttributes(Map<String, Object> attr) {
        CONTEXT.get().getContext().putAll(attr);
    }

    public static Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(CONTEXT.get().getContext());
    }

}
