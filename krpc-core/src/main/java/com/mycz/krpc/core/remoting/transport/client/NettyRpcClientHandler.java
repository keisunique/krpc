package com.mycz.krpc.core.remoting.transport.client;

import com.mycz.krpc.core.factory.ApplicationContext;
import com.mycz.krpc.core.remoting.entity.RpcConstants;
import com.mycz.krpc.core.remoting.entity.RpcMessage;
import com.mycz.krpc.core.remoting.entity.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NettyRpcClientHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            if (msg instanceof RpcMessage tmp) {
                byte messageType = tmp.getMessageType();
                if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                } else if (messageType == RpcConstants.RESPONSE_TYPE) {
                    RpcResponse<Object> rpcResponse = (RpcResponse<Object>) tmp.getData();
                    UnprocessedRequests unprocessedRequests = ApplicationContext.getInstance(UnprocessedRequests.class);
                    unprocessedRequests.complete(rpcResponse);
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

}