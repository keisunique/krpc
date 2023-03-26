package com.mycz.krpc.core.remoting.transport.client;

import com.esotericsoftware.minlog.Log;
import com.mycz.krpc.core.remoting.entity.RpcMessage;
import com.mycz.krpc.core.remoting.entity.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class UnprocessedRequests {

    private final Map<Integer, CompletableFuture<RpcResponse<Object>>> futureMap = new ConcurrentHashMap<>();

    public void put(int traceId, CompletableFuture<RpcResponse<Object>> future) {
        futureMap.put(traceId, future);
    }

    public void complete(RpcMessage rpcMessage) {
        CompletableFuture<RpcResponse<Object>> future = futureMap.remove(rpcMessage.getTraceId());
        if (future != null) {
            future.complete((RpcResponse<Object>) rpcMessage.getData());
        } else {
            throw new IllegalStateException();
        }
    }

}
