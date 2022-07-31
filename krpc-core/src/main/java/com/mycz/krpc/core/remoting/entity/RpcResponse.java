package com.mycz.krpc.core.remoting.entity;

import lombok.*;
import org.apache.poi.ss.formula.functions.T;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse<T>  implements Serializable {

    private String requestId;
    /**
     * response code
     */
    private Integer code;
    /**
     * response message
     */
    private String message;
    /**
     * response body
     */
    private T data;

    public static <T> RpcResponse<T> success(T data, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(1);
        response.setMessage("成功");
        response.setRequestId(requestId);
        if (null != data) {
            response.setData(data);
        }
        return response;
    }


    public static <T> RpcResponse<T> fail() {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(0);
        response.setMessage("失败");
        return response;
    }


}