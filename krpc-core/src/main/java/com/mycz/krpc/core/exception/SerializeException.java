package com.mycz.krpc.core.exception;

/**
 * 序列化异常
 */
public class SerializeException extends RuntimeException {
    public SerializeException(String message, Throwable e) {
        super(message, e);
    }
}
