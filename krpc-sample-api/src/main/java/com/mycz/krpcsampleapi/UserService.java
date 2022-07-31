package com.mycz.krpcsampleapi;

import com.mycz.krpc.core.annotation.KrpcReference;

import java.util.List;

@KrpcReference(serviceName = "UserService")
public interface UserService {

    String sayHello();

    List<String> sayGoodBye(String name);

}
