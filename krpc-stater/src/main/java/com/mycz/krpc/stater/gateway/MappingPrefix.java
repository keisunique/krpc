package com.mycz.krpc.stater.gateway;

import lombok.Getter;

/**
 * 请求路径前缀
 */
public enum MappingPrefix {

    MAPI("mapi"),
    API("api");

    @Getter
    private final String prefix;

    MappingPrefix(String prefix) {
        this.prefix = prefix;
    }

}
