package com.mycz.krpc.sample.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseRequestHeader {
    private String ip;
    private String context;
    private String traceId;
    private String path;
    private String authorization;
}
