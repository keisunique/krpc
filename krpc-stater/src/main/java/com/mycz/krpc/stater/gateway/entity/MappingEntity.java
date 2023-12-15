package com.mycz.krpc.stater.gateway.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mycz.arch.common.gateway.RequestMethod;
import com.mycz.krpc.stater.gateway.annotation.ResponseType;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MappingEntity {
    // 名称
    private String name;
    // 路径
    private String path;
    // 请求方式
    private RequestMethod method;
    // 是否需要权限
    private boolean authority;
    // 服务
    private Service service;
    // 描述
    private String description;
    // 响应数据类型, 默认API类型
    private ResponseType responseType = ResponseType.API;
    // 是否传递原始http请求体
    private boolean deliverPayload = false;

    // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;


    /**
     * 参数
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Parameter {
        private String name;
        private String type;
    }

    /**
     * 服务
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Service {
        private String name;
        private String clazz;
        private String method;
        private String paramType; // 参数类型
    }
}
