package com.mycz.krpc.stater.gateway.annotation;

import lombok.Getter;

public enum MappingType {

    API("api"),
    MAPI("mapi");

    @Getter
    private final String path;

    MappingType(String path) {
        this.path = path;
    }

}
