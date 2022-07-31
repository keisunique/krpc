package com.mycz.krpc.core.remoting.entity;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class RpcConstants {

    /**
     * 魔数, 用于校验来自于krpc的消息
     */
    public static final byte[] MAGIC_NUMBER = {(byte) 'k', (byte) 'r', (byte) 'p', (byte) 'c'};

    /**
     * 版本号
     */
    public static final byte VERSION = 1;

    /**
     * 消息头部长度
     */
    public static final int HEAD_LENGTH = 16;

    /**
     * 请求
     */
    public static final byte REQUEST_TYPE = 1;

    /**
     * 响应
     */
    public static final byte RESPONSE_TYPE = 2;

    /**
     * ping
     */
    public static final byte HEARTBEAT_REQUEST_TYPE = 3;

    /**
     * pong
     */
    public static final byte HEARTBEAT_RESPONSE_TYPE = 4;


    /**
     * 编码格式
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;



}
