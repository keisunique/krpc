package com.mycz.krpcsampleapi;

import com.mycz.krpc.core.annotation.KrpcReference;

@KrpcReference(serviceName = "UserService")
public interface OrderService {

    int total();

}
