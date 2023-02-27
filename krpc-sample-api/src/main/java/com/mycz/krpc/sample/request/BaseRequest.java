package com.mycz.krpc.sample.request;


import lombok.*;

public abstract class BaseRequest {

    private Header header;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        private String ip;
        private String context;
        private String requestId;
    }
}
