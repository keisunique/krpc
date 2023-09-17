package com.mycz.krpc.core.remoting.transport.server;

import com.mycz.arch.common.util.JsonKit;
import com.mycz.krpc.core.config.RpcConfig;
import com.mycz.krpc.core.factory.ApplicationContext;
import com.mycz.krpc.core.remoting.transport.codec.RpcMessageDecoder;
import com.mycz.krpc.core.remoting.transport.codec.RpcMessageEncoder;
import com.mycz.krpc.core.utils.RuntimeUtil;
import com.mycz.krpc.core.utils.ThreadPoolFactoryUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NettyRpcServer {

    @SneakyThrows
    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                RuntimeUtil.cpus() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );

        log.info("*** krpc - 开始启动");
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    //表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 512)
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast("server-idle-handler", new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            p.addLast("encoder", new RpcMessageEncoder());
                            p.addLast("decoder", new RpcMessageDecoder());
                            p.addLast(serviceHandlerGroup, new NettyRpcServerHandler());
                        }
                    });

            RpcConfig rpcConfig = ApplicationContext.getInstance(RpcConfig.class);
            ChannelFuture f = b.bind(rpcConfig.getHost(), rpcConfig.getPort() == null ? 0 : rpcConfig.getPort()).sync();
            f.addListener(new NettyRpcServiceListener());

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("*** krpc - 关闭");
                ThreadPoolFactoryUtil.shutDownAllThreadPool();
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
                serviceHandlerGroup.shutdownGracefully();
            }));
        } catch (Exception e) {
            log.error("*** krpc - 启动异常", e);
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
        }
    }

}
