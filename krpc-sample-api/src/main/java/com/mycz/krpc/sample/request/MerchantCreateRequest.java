package com.mycz.krpc.sample.request;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MerchantCreateRequest extends BaseRequest{

    private String name;
    private String address;

}
