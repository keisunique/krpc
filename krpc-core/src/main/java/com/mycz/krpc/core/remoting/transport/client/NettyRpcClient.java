package com.mycz.krpc.core.remoting.transport.client;

import com.mycz.arch.common.util.JsonKit;
import com.mycz.krpc.core.factory.ApplicationContext;
import com.mycz.krpc.core.registry.ServiceDiscovery;
import com.mycz.krpc.core.registry.consul.ConsulServiceDiscovery;
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
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyRpcClient {

    private final ChannelProvider channelProvider;
    private final Bootstrap bootstrap;
    private final UnprocessedRequests unprocessedRequests;

    public NettyRpcClient() {
        channelProvider = new ChannelProvider();
        bootstrap = new Bootstrap();

        NioEventLoopGroup workerGroup = new NioEventLoopGroup();

        bootstrap.group(workerGroup) // 1.指定线程模型
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

        unprocessedRequests = new UnprocessedRequests();
        ApplicationContext.addInstance(UnprocessedRequests.class, unprocessedRequests);
    }

    public CompletableFuture<RpcResponse<Object>> sendRpcRequest(RpcRequest rpcRequest) throws ExecutionException, InterruptedException {
        // 发现服务
        ServiceDiscovery serviceDiscovery = new ConsulServiceDiscovery();
        ServiceDiscoveryResult service = serviceDiscovery.discovery(rpcRequest.getServiceName());

        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();
        Channel channel = getChannel(new InetSocketAddress(service.getAddress(), service.getPort()));
        if (channel.isActive()) {
            // 封装rpcMessage
            RpcMessage rpcMessage = RpcMessage.builder()
                    .magicNum(RpcConstants.MAGIC_NUMBER)
                    .version((byte) 1)
                    .messageType((byte) 1)
                    .codec((byte) 1)
                    .compress((byte) 1)
                    .data(rpcRequest)
                    .build();

            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
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

    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        RpcRequest rpcRequest = RpcRequest.builder()
                .requestId("1111")
                .version("1")
                .interfaceName("com.mycz.krpcsampleapi.UserService")
                .methodName("sayHello")
                .paramTypes(new Class<?>[0])
                .parameters(new Object[0])
                .build();

        CompletableFuture<RpcResponse<Object>> future = new NettyRpcClient().sendRpcRequest(rpcRequest);
        System.out.println(JsonKit.toJson(future.get()));
    }
}
