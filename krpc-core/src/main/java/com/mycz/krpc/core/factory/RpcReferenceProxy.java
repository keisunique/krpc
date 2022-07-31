package com.mycz.krpc.core.factory;

import com.mycz.krpc.core.remoting.entity.RpcRequest;
import com.mycz.krpc.core.remoting.entity.RpcResponse;
import com.mycz.krpc.core.remoting.transport.client.NettyRpcClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class RpcReferenceProxy {

    /**
     * 代理rpc接口
     */
    public static Object proxy(String serviceName, String interfaceName, String methodName, Class<?>[] paramTypes, Object[] parameters) throws ExecutionException, InterruptedException {
        // 封装rpc请求消息
        RpcRequest rpcRequest = RpcRequest.builder()
                .requestId("1111")
                .version("1")
                .serviceName(serviceName)
                .interfaceName(interfaceName)
                .methodName(methodName)
                .paramTypes(paramTypes)
                .parameters(parameters)
                .build();

        CompletableFuture<RpcResponse<Object>> future = new NettyRpcClient().sendRpcRequest(rpcRequest);
        return future.get().getData();
    }

}
