package com.mycz.krpc.core.factory;

import com.mycz.arch.common.util.RandomKit;
import com.mycz.krpc.core.remoting.entity.RpcRequest;
import com.mycz.krpc.core.remoting.entity.RpcResponse;
import com.mycz.krpc.core.remoting.transport.client.NettyRpcClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RpcReferenceProxy {

    /**
     * 代理rpc接口
     */
    public static Object proxy(String serviceName, String interfaceName, String methodName, Class<?>[] paramTypes, Object[] parameters) throws Exception {
        // 封装rpc请求消息
        RpcRequest rpcRequest = RpcRequest.builder()
                .traceId(RandomKit.randomNumStr(16))
                .ip("ip")
                .version("1.0")
                .serviceName(serviceName)
                .interfaceName(interfaceName)
                .methodName(methodName)
                .paramTypes(paramTypes)
                .parameters(parameters)
                .build();

        NettyRpcClient client = ApplicationContext.getInstance(NettyRpcClient.class);
        CompletableFuture<RpcResponse<Object>> future = client.sendRpcRequest(rpcRequest);
        return future.get(5, TimeUnit.SECONDS).getData();
    }

}
