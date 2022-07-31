package com.mycz.krpc.core.serializer;

/**
 * 序列化接口，所有序列化类都要实现这个接口
 */
public interface Serializer {

    /**
     * 序列化
     */
    byte[] serialize(Object obj);

    /**
     * 反序列化
     */
    <T> T deserialize(byte[] bytes, Class<T> clazz);

}
