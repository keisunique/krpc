package com.mycz.krpc.core;

import com.mycz.arch.common.util.StringKit;
import com.mycz.krpc.core.config.RpcConfig;
import com.mycz.krpc.core.factory.ApplicationContext;
import com.mycz.krpc.core.registry.ServiceDiscovery;
import com.mycz.krpc.core.registry.ServiceRegistry;
import com.mycz.krpc.core.registry.consul.ConsulServiceDiscovery;
import com.mycz.krpc.core.registry.consul.ConsulServiceRegistry;
import com.mycz.krpc.core.remoting.entity.RpcRequest;
import com.mycz.krpc.core.remoting.transport.client.NettyRpcClient;
import com.mycz.krpc.core.remoting.transport.client.UnprocessedRequests;
import com.mycz.krpc.core.remoting.transport.compress.gzip.GzipCompress;
import com.mycz.krpc.core.remoting.transport.server.NettyRpcServer;
import com.mycz.krpc.core.serializer.kryo.KryoSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 轻量高效的Java RPC微服务框架
 */
public class KrpcApplication {

    /**
     * rpc配置
     */
    private RpcConfig rpcConfig;

    public KrpcApplication(RpcConfig rpcConfig) {
        this.rpcConfig = rpcConfig;
    }

    /**
     * 启动服务
     */
    public void start() {
        // 1. 校验并填充配置项
        this.checkAndFillRpcConfig();
        // 2.
        this.initApplicationContext();
        // 3. 启动服务
        new NettyRpcServer().start();
    }

    /**
     * 初始化需要用到的对象,放入全局容器中
     */
    public void initApplicationContext() {
        ApplicationContext.addInstance(RpcConfig.class, rpcConfig);
        if (rpcConfig.getRegistry().getEnable()) {
            ApplicationContext.addInstance(ServiceRegistry.class, new ConsulServiceRegistry(rpcConfig.getRegistry().getAddress()));
            ApplicationContext.addInstance(ServiceDiscovery.class, new ConsulServiceDiscovery(rpcConfig.getRegistry().getAddress()));
        }
        ApplicationContext.addInstance(NettyRpcClient.class, new NettyRpcClient());
        ApplicationContext.addInstance(UnprocessedRequests.class, new UnprocessedRequests());
    }

    /**
     * 校验并填充配置项
     */
    public void checkAndFillRpcConfig() {
        // 基础信息
        if (rpcConfig == null) {
            rpcConfig = new RpcConfig();
        }
        if (StringKit.isBlank(rpcConfig.getHost())) {
            rpcConfig.setHost("127.0.0.1");
        }
        if (rpcConfig.getPort() == null) {
            rpcConfig.setPort(0);
        }
        if (StringKit.isBlank(rpcConfig.getName())) {
            throw new IllegalArgumentException("`krpc:name` 必填");
        }

        // 注册中心
        RpcConfig.Registry registry = rpcConfig.getRegistry();
        if (registry == null) {
            registry = new RpcConfig.Registry();
            rpcConfig.setRegistry(registry);
        }
        if (registry.getEnable() == null) {
            registry.setEnable(false);
        }
        if (registry.getEnable()) {
            // 注册中心启用时,检查具体配置
            if (StringKit.isBlank(registry.getAddress())) {
                throw new IllegalArgumentException("`krpc:registry:address` 必填");
            }
        }

    }


    public static void main(String[] args) {

        // 测试序列化
        KryoSerializer kyro = new KryoSerializer();

        List<RpcRequest> list = new ArrayList<>();

        for (int i = 0; i < 20; i++) {
            list.add(RpcRequest.builder()
                    .traceId("111")
                    .version("v1")
                    .interfaceName("UserService")
                    .methodName("list")
                    .group("v1")
                    .paramTypes(new Class<?>[]{Integer.class, Integer.class})
                    .parameters(new Object[]{1, 2})
                    .build());
        }

//        long begin = System.currentTimeMillis();
//        for (int i = 0; i < 10000; i++) {
//            byte[] serialize = kyro.serialize(request);
//            kyro.deserialize(serialize, RpcRequest.class);
//        }
//        System.out.println("use = " + (System.currentTimeMillis() - begin));

        byte[] serialize = kyro.serialize(list);
        System.out.println("Arrays.toString(serialize)");
        System.out.println("压缩前大小 = " + serialize.length);

        byte[] compress = new GzipCompress().compress(serialize);
        System.out.println(Arrays.toString(compress));
        System.out.println("压缩后大小 = " + compress.length);

        System.out.println(Arrays.toString(new GzipCompress().decompress(compress)));

//        String s = Arrays.toString(serialize);
////
//        byte[] ss = new byte[]{1, 7, 103, 114, 111, 117, -16, 105, 110, 116, 101, 114, 102, 97, 99, 101, 78, 97, 109, -27, 109, 101, 116, 104, 111, 100, 78, 97, 109, -27, 112, 97, 114, 97, 109, 84, 121, 112, 101, -13, 112, 97, 114, 97, 109, 101, 116, 101, 114, -13, 114, 101, 113, 117, 101, 115, 116, 73, -28, 118, 101, 114, 115, 105, 111, -18, 0, 0, 0, 0, 0, 3, 1, 49, 49, 49, -79};
////
//        System.out.println(JsonKit.toJson(kyro.deserialize(serialize, RpcRequest.class)));


    }
}
