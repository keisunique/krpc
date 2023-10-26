package com.mycz.krpc.core.remoting.transport.client;

import com.mycz.arch.common.util.RandomKit;
import com.mycz.arch.common.util.StringKit;
import com.mycz.krpc.core.factory.ApplicationContext;
import com.mycz.krpc.core.registry.ServiceDiscovery;
import com.mycz.krpc.core.registry.entity.ServiceDiscoveryResult;
import com.mycz.krpc.core.remoting.entity.RpcConstants;
import com.mycz.krpc.core.remoting.entity.RpcMessage;
import com.mycz.krpc.core.remoting.entity.RpcRequest;
import com.mycz.krpc.core.remoting.entity.RpcResponse;
import com.mycz.krpc.core.remoting.transport.codec.RpcMessageDecoder;
import com.mycz.krpc.core.remoting.transport.codec.RpcMessageEncoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class NettyRpcClient {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;

    public NettyRpcClient() {
        channelProvider = new ChannelProvider();
        bootstrap = new Bootstrap();

        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        bootstrap.group(workerGroup) // 1.指定线程模型
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 设置连接超时时间
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .channel(NioSocketChannel.class) // 2.指定 IO 类型为 NIO
                .handler(new ChannelInitializer<SocketChannel>() { // 3.IO 处理逻辑
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new IdleStateHandler(0, 5, 0, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new RpcMessageEncoder());
                        ch.pipeline().addLast(new RpcMessageDecoder());
                        ch.pipeline().addLast(new NettyRpcClientHandler());
                    }
                });
    }

    public CompletableFuture<RpcResponse<Object>> sendRpcRequest(RpcRequest rpcRequest) throws Exception {
        // 发现服务
        ServiceDiscovery serviceDiscovery = ApplicationContext.getInstance(ServiceDiscovery.class);
        ServiceDiscoveryResult service = serviceDiscovery.discovery(rpcRequest.getServiceName());

        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        Channel channel = getChannel(new InetSocketAddress(service.getAddress(), service.getPort()));

        if (channel.isActive()) {
            // 携带上下文和ip
            rpcRequest.setContext(new HashMap<>(ApplicationContext.getContext()));
            rpcRequest.setIp(ApplicationContext.getIp());
            if (StringKit.isBlank(rpcRequest.getTraceId())) {
                rpcRequest.setTraceId(RandomKit.randomNumStr(16));
            }

            // 封装rpcMessage
            RpcMessage rpcMessage = RpcMessage.builder()
                    .magicNum(RpcConstants.MAGIC_NUMBER)
                    .version(RpcConstants.VERSION)
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .codec(RpcConstants.CODEC_KRYO)
                    .compress(RpcConstants.COMPRESS_GZIP)
                    .traceId(ATOMIC_INTEGER.getAndIncrement())
                    .data(rpcRequest)
                    .build();

            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                UnprocessedRequests unprocessedRequests = ApplicationContext.getInstance(UnprocessedRequests.class);
                unprocessedRequests.put(rpcMessage.getTraceId(), resultFuture);
                if (!future.isSuccess()) {
                    future.channel().close();
                    resultFuture.completeExceptionally(future.cause());
                    log.error("请求发送失败", future.cause());
                }
            });
        } else {
            log.error("Channel 已关闭");
        }

        return resultFuture;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelProvider.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });

        Channel channel;
        try {
            channel = completableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }

        return channel;
    }

}
