package com.mycz.krpc.stater.gateway.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mycz.krpc.stater.gateway.annotation.MappingType;
import com.mycz.krpc.stater.gateway.annotation.RequestMethod;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@Builder
@JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MappingEntity {

    // 名称
    private String name;
    // 路径
    private String path;
    // 请求方式
    private RequestMethod method;
    // 请求参数
    private List<Parameter> parameters;
    // 是否需要权限
    private boolean authority;
    // 服务
    private Service service;
    // 描述
    private String description;

    // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;


    /**
     * 参数
     */
    @Getter
    @Setter
    @Builder
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
    public static class Service {
        private String name;
        private String clazz;
        private String method;
    }

}
