package com.mycz.krpc.core.factory;

import com.mycz.arch.common.util.JsonKit;

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
            .traceId("")
            .context(new HashMap<>())
            .build());

    public static String getIp() {
        return CONTEXT.get().getIp();
    }

    public static void setIp(String ip) {
        CONTEXT.get().setIp(ip);
    }

    public static String getTraceId() {
        return CONTEXT.get().getTraceId();
    }

    public static void setTranceId(String tranceId) {
        CONTEXT.get().setTraceId(tranceId);
    }

    public static void addAttribute(String key, Object value) {
        CONTEXT.get().getContext().put(key, value);
    }

    public static Object getAttribute(String key) {
        return CONTEXT.get().getContext().get(key);
    }

    public static void addAttributes(Map<String, Object> attr) {
        System.out.println("attr :  "+JsonKit.toJson(attr));
        System.out.println("context :  "+JsonKit.toJson(CONTEXT.get()));
        CONTEXT.get().getContext().putAll(attr);
    }

    public static Map<String, Object> getContext() {
        return CONTEXT.get().getContext();
    }

}
