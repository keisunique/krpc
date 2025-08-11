package com.mycz.krpc.stater.document.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUploadReq {
    private List<Item> apiInfos;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        /**
         * 服务名称
         */
        private String serviceName;

        /**
         * api类路径
         */
        private String className;

        /**
         * 标签
         */
        private String tag;

        /**
         * 接口名称
         */
        private String name;

        /**
         * 请求方式
         */
        private String method;

        /**
         * 请求路径
         */
        private String path;

        /**
         * 前缀
         */
        private String prefix;

        /**
         * 授权认证
         */
        private Short authority;

        /**
         * 授权认证方式
         */
        private String authorityType;

        /**
         * 描述
         */
        private String description;

        /**
         * 响应类型
         */
        private String responseType;

        /**
         * 请求中带有_payload参数
         */
        private Short deliverPayload;

        /**
         * 请求中带有_params参数
         */
        private Short deliverParams;

        /**
         * 请求参数类路径名
         */
        private String requestClass;

        /**
         * 响应参数类路径名
         */
        private String responseClass;

        /**
         * 接口参数列表
         */
        private List<Params> params;


        @Getter
        @Setter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Params {
            /**
             * 字段名
             */
            private String field;

            /**
             * 参数类型（request、response）
             */
            private String type;

            /**
             * 字段类路径
             */
            private String fieldClass;

            /**
             * 是否必填(0：否，1：是)
             */
            private Short required;

            /**
             * 描述
             */
            private String description;
        }
    }
}
