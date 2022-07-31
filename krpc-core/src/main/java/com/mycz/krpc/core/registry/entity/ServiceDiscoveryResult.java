package com.mycz.krpc.core.registry.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ServiceDiscoveryResult {

    private String id;
    private String name;
    private String address;
    private Integer port;

}
