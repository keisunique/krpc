package com.mycz.krpc.core.remoting.transport.client;

import com.esotericsoftware.minlog.Log;
import com.mycz.krpc.core.remoting.entity.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UnprocessedRequests {

    private final Map<String, CompletableFuture<RpcResponse<Object>>> futureMap = new ConcurrentHashMap<>();

    public void put(String traceId, CompletableFuture<RpcResponse<Object>> future) {
        futureMap.put(traceId, future);
    }

    public void complete(RpcResponse<Object> rpcResponse) {
        CompletableFuture<RpcResponse<Object>> future = futureMap.remove(rpcResponse.getTraceId());
        if (future != null) {
            future.complete(rpcResponse);
        } else {
            throw new IllegalStateException();
        }
    }

}
