package com.mycz.krpc.core.remoting.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcResponse<T>  implements Serializable {

    private String tranceId;
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

    public static <T> RpcResponse<T> success(T data, String tranceId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(1);
        response.setMessage("成功");
        response.setTranceId(tranceId);
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
