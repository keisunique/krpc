package com.mycz.krpc.core.provider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * rpc方法调用
 */
public class RpcReferenceInvoke {

    private static final ConcurrentHashMap<String, Object> INTERFACE_MAP = new ConcurrentHashMap<>();

    /**
     * 根据接口名，方法，参数值 调用对应的实现方法
     */
    public static Object invoke(String interfaceName, String methodName, Class<?>[] paramTypes, Object[] parameters) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object interfaceObject = INTERFACE_MAP.get(interfaceName);
//        System.out.println("method : " + methodName + " interfaceName : " + interfaceName + " paramtype : " + paramTypes[0].getName());
        Method method = interfaceObject.getClass().getMethod(methodName, paramTypes);

        return method.invoke(interfaceObject, parameters);
    }

    public static void addInterface(Class<?> tclass, Object obj) {
        INTERFACE_MAP.put(tclass.getName(), obj);
    }

}
