package com.mycz.krpc.core.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.mycz.krpc.core.exception.SerializeException;
import com.mycz.krpc.core.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Slf4j
public class KryoSerializer implements Serializer {
    
    /**
     * kryo非线程安全，所以使用ThreadLocal来维护一份kryo实例
     */
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);//默认值为true，避免版本变化显式设置
        kryo.setReferences(true);//检测循环依赖，默认值为true,避免版本变化显式设置
        kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
        ((DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy()).setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());//设定默认的实例化器
        return kryo;
    });

    @Override
    public byte[] serialize(Object obj) {
        try (ByteArrayOutputStream arrayOs = new ByteArrayOutputStream()) {
            Output output = new Output(arrayOs);
            Kryo kryo = kryoThreadLocal.get();
            kryo.writeObjectOrNull(output, obj, obj.getClass());
            output.flush();
            return arrayOs.toByteArray();
        } catch (Exception e) {
            throw new SerializeException("序列化失败: " + e.getMessage(), e);
        } finally {
            kryoThreadLocal.remove();
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> clazz) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             Input input = new Input(byteArrayInputStream)) {
            Kryo kryo = kryoThreadLocal.get();
            return clazz.cast(kryo.readObject(input, clazz));
        } catch (Exception e) {
            throw new SerializeException("反序列化失败: " + e.getMessage(), e);
        } finally {
            kryoThreadLocal.remove();
        }
    }

}
