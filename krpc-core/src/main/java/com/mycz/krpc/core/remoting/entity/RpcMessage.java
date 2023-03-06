package com.mycz.krpc.core.remoting.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RpcMessage {

    /**
     * 魔数
     */
    private byte[] magicNum;

    /**
     * 版本号
     */
    private byte version;

    /**
     * 消息类型 1-请求， 2-响应， 3-心跳请求， 4-心跳响应
     */
    private byte messageType;

    /**
     * serialization type
     */
    private byte codec;

    /**
     * compress type
     */
    private byte compress;

    /**
     * trace id
     */
    private int traceId;

    /**
     * request | response data
     */
    private Object data;


}
