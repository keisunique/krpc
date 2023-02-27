package com.mycz.dubboprovider.impl;

import com.esotericsoftware.minlog.Log;
import com.mycz.arch.common.gateway.RequestMethod;
import com.mycz.arch.common.util.JsonKit;
import com.mycz.krpc.sample.api.MerchantService;
import com.mycz.krpc.sample.request.MerchantCreateRequest;
import com.mycz.krpc.sample.response.MerchantCreateResponse;
import com.mycz.krpc.stater.gateway.annotation.RequestMapping;
import org.springframework.stereotype.Service;

@Service
public class MerchantServiceImpl implements MerchantService {

    @RequestMapping(method = RequestMethod.POST, paths = "/rest/1.0/merchant/create", name = "创建商户", description = "描述")
    @Override
    public MerchantCreateResponse create(MerchantCreateRequest request) {

        Log.info("MerchantCreateRequest = {}", JsonKit.toJson(request));

        return null;
    }
}
