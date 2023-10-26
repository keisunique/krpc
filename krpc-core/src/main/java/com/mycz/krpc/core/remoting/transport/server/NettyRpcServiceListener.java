package com.mycz.krpc.core.remoting.transport.server;

import com.mycz.krpc.core.config.RpcConfig;
import com.mycz.krpc.core.factory.ApplicationContext;
import com.mycz.krpc.core.registry.ServiceRegistry;
import com.mycz.krpc.core.registry.consul.ConsulServiceRegistry;
import com.mycz.krpc.core.registry.entity.ServiceRegisterResult;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Slf4j
public class NettyRpcServiceListener implements ChannelFutureListener {

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
            log.info("*** krpc - 启动成功");

            InetSocketAddress socketAddress = (InetSocketAddress) future.channel().localAddress();
            log.info("*** krpc - 端口: {}", socketAddress.getPort());

            // 注册服务
            RpcConfig rpcConfig = ApplicationContext.getInstance(RpcConfig.class);
            rpcConfig.setPort(socketAddress.getPort());
            if (rpcConfig.getRegistry().getEnable()) {
                this.register(rpcConfig.getName(), rpcConfig.getHost(), rpcConfig.getPort());
            }
        } else {
            throw new Exception("*** krpc - 启动失败");
        }
    }

    /**
     * 注册服务
     */
    private void register(String serviceName, String host, int port) {
        ServiceRegistry registry = ApplicationContext.getInstance(ServiceRegistry.class);
        ServiceRegisterResult result = registry.register(serviceName, new InetSocketAddress(host, port));
        // 服务关闭时自动解除注册
        Runtime.getRuntime().addShutdownHook(new Thread(() -> registry.deregister(result.getId())));
    }

}
