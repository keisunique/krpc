package com.mycz.krpc.core.remoting.transport.codec;

import com.mycz.krpc.core.remoting.entity.RpcConstants;
import com.mycz.krpc.core.remoting.entity.RpcMessage;
import com.mycz.krpc.core.remoting.entity.RpcRequest;
import com.mycz.krpc.core.remoting.entity.RpcResponse;
import com.mycz.krpc.core.remoting.transport.compress.gzip.GzipCompress;
import com.mycz.krpc.core.serializer.kryo.KryoSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * rpc消息解码
 */
@Slf4j
public class RpcMessageDecoder extends LengthFieldBasedFrameDecoder {

    public RpcMessageDecoder() {
        this(8 * 1024 * 1024, 12, 4, -16, 0);
    }

    public RpcMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                             int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            super.channelRead(ctx, msg);
        } catch (Exception e) {
            log.error("[RpcMessageDecoder][ChannelRead] - ", e);
//            ctx.fireExceptionCaught(e);
        }
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object decoded = super.decode(ctx, in);
        if (decoded instanceof ByteBuf frame) {
            if (frame.readableBytes() >= 16) {
                try {
                    return decodeFrame(frame);
                } finally {
                    frame.release();
                }
            }
        }
        return decoded;
    }

    private Object decodeFrame(ByteBuf in) {
        // 检查魔数
        checkMagicNumber(in);

        RpcMessage rpcMessage = RpcMessage.builder()
                .magicNum(RpcConstants.MAGIC_NUMBER)
                .version(in.readByte())
                .messageType(in.readByte())
                .codec(in.readByte())
                .compress(in.readByte())
                .traceId(in.readInt())
                .build();

        int fullLength = in.readInt();
        int bodyLength = fullLength - RpcConstants.HEAD_LENGTH;
        byte[] body = new byte[bodyLength];
        in.readBytes(body);
        byte[] decompressBody = new GzipCompress().decompress(body);

        Object data = null;
        try {
            if (RpcConstants.REQUEST_TYPE == rpcMessage.getMessageType()) {
                data = new KryoSerializer().deserialize(decompressBody, RpcRequest.class);
            } else if (RpcConstants.RESPONSE_TYPE == rpcMessage.getMessageType()) {
                data = new KryoSerializer().deserialize(decompressBody, RpcResponse.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        rpcMessage.setData(data);

        return rpcMessage;
    }

    private void checkMagicNumber(ByteBuf in) {
        int len = RpcConstants.MAGIC_NUMBER.length;
        byte[] tmp = new byte[len];
        in.readBytes(tmp);
        for (int i = 0; i < len; i++) {
            if (tmp[i] != RpcConstants.MAGIC_NUMBER[i]) {
                throw new IllegalArgumentException("Unknown magic code: " + Arrays.toString(tmp));
            }
        }
    }


}
