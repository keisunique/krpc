package com.mycz.krpc.sample.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantCreateResponse{

    private Long id;
    private String name;

}
