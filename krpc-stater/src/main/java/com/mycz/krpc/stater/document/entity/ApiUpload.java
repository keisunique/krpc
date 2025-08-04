package com.mycz.krpc.stater.document.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiUpload {
    private ApiInfo apiInfo;
    private List<ApiInterfaceInfo> apiInterfaceInfos;
}
