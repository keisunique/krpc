package com.mycz.krpc.stater.document.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiInfo {
    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * api类路径
     */
    private String apiClass;

    /**
     * 标签
     */
    private String tags;

    /**
     * 描述
     */
    private String description;
}
