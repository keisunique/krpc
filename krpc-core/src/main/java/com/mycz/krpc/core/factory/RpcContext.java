package com.mycz.krpc.core.factory;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RpcContext {

    /**
     * ip
     */
    private String ip;

    /**
     * 请求id
     */
    private String requestId;

    /**
     * 上下文内容
     */
    private Map<String,Object> context;
}
