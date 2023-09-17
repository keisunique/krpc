package com.mycz.krpc.core.remoting.transport.codec;

import com.mycz.krpc.core.remoting.entity.RpcConstants;
import com.mycz.krpc.core.remoting.entity.RpcMessage;
import com.mycz.krpc.core.remoting.transport.compress.gzip.GzipCompress;
import com.mycz.krpc.core.serializer.kryo.KryoSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 * rpc消息编码
 */
@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf out) {
        try {
            // 消息头
            out.writeBytes(rpcMessage.getMagicNum()); // 4B
            out.writeByte(rpcMessage.getVersion()); // 1B
            out.writeByte(rpcMessage.getMessageType()); // 1B
            out.writeByte(rpcMessage.getCodec()); // 1B
            out.writeByte(rpcMessage.getCompress()); // 1B
            out.writeInt(rpcMessage.getTraceId()); // 4B

            byte[] bodyBytes = null;
            int fullLength = out.writerIndex() + 4;
            if (rpcMessage.getMessageType() != RpcConstants.HEARTBEAT_REQUEST_TYPE && rpcMessage.getMessageType() != RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
                bodyBytes = new KryoSerializer().serialize(rpcMessage.getData());
                bodyBytes = new GzipCompress().compress(bodyBytes);
                fullLength += bodyBytes.length;
            }

            // 消息总长度
            out.writeInt(fullLength);

            // 消息体
            if (bodyBytes != null) {
                out.writeBytes(bodyBytes);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
