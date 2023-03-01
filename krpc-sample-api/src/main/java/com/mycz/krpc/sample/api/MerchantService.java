package com.mycz.krpc.sample.api;

import com.mycz.krpc.core.annotation.KrpcReference;
import com.mycz.krpc.sample.request.MerchantCreateRequest;
import com.mycz.krpc.sample.response.MerchantCreateResponse;
import lombok.Getter;
import lombok.Setter;

@KrpcReference(serviceName = "UserService")
public interface MerchantService {

    /**
     * 创建商户
     */
    MerchantCreateResponse create(MerchantCreateRequest request);

}
