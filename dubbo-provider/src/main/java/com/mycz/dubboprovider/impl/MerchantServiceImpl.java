package com.mycz.dubboprovider.impl;

import com.esotericsoftware.minlog.Log;
import com.mycz.arch.common.gateway.RequestMethod;
import com.mycz.arch.common.util.JsonKit;
import com.mycz.krpc.sample.api.MerchantService;
import com.mycz.krpc.sample.request.MerchantCreateRequest;
import com.mycz.krpc.sample.response.MerchantCreateResponse;
import com.mycz.krpc.stater.gateway.annotation.RequestMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MerchantServiceImpl implements MerchantService {

    @RequestMapping(method = RequestMethod.POST, paths = "/rest/1.0/merchant/create", name = "创建商户", description = "描述")
    @Override
    public MerchantCreateResponse create(MerchantCreateRequest request) {
        log.info("MerchantCreateRequest = {}", JsonKit.toJson(request));

        return MerchantCreateResponse.builder().id(1L).name("22").build();
    }
}
