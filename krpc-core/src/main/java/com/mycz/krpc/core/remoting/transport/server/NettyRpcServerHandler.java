package com.mycz.krpc.core.remoting.transport.server;

import com.mycz.arch.common.util.JsonKit;
import com.mycz.krpc.core.factory.ApplicationContext;
import com.mycz.krpc.core.provider.RpcReferenceInvoke;
import com.mycz.krpc.core.remoting.entity.RpcConstants;
import com.mycz.krpc.core.remoting.entity.RpcMessage;
import com.mycz.krpc.core.remoting.entity.RpcRequest;
import com.mycz.krpc.core.remoting.entity.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;

@Slf4j
public class NettyRpcServerHandler extends ChannelInboundHandlerAdapter {

    private static final DefaultEventLoopGroup eventLoopGroup = new DefaultEventLoopGroup(16);

    /**
     * 处理接收数据
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        eventLoopGroup.execute(() -> {
            try {
                if (msg instanceof RpcMessage rpcMessage) {
                    // 心跳请求
                    if (RpcConstants.HEARTBEAT_REQUEST_TYPE == rpcMessage.getMessageType()) {
                        rpcMessage.setMessageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE);
                    }

                    // 常规请求
                    Object data = rpcMessage.getData();
                    if (data instanceof RpcRequest rpcRequest) {
                        // 全局上下文
                        ApplicationContext.setTranceId(rpcRequest.getTraceId());
                        ApplicationContext.setIp(rpcRequest.getIp());
                        ApplicationContext.addAttributes(rpcRequest.getContext());

                        // 找到实际要调用的类
                        try {
                            ThreadContext.put("TRACE_ID", rpcRequest.getTraceId());
                            Object result = RpcReferenceInvoke.invoke(rpcRequest.getInterfaceName(), rpcRequest.getMethodName(), rpcRequest.getParamTypes(), rpcRequest.getParameters());
                            rpcMessage.setData(RpcResponse.success(result, rpcRequest.getTraceId()));
                        } catch (Exception e) {
                            RpcResponse<Object> response = RpcResponse.fail();
                            response.setTraceId(rpcRequest.getTraceId());
                            rpcMessage.setData(response);
                            log.error("not writable now, message dropped", e);
                        } finally {
                            ThreadContext.remove("TRACE_ID");
                        }
                    } else {
                        log.error("非法rpc请求内容, {}", JsonKit.toJson(data));
                    }
                    rpcMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                } else {
                    log.info("[NettyRpcServerHandler][ChannelRead] - invalid msg");
                }
            } finally {
                // 确保ByteBuf释放, 防止内存泄漏
                ReferenceCountUtil.release(msg);
            }
        });

    }

    /**
     * 处理心跳事件
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent event) {
            if (event.state() == IdleState.READER_IDLE) {
                ctx.channel().close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("server catch exception", cause);
        ctx.close();
    }
}
