package com.mycz.dubboprovider.impl;

import com.google.common.collect.Lists;
import com.mycz.krpc.stater.gateway.annotation.RequestMapping;
import com.mycz.krpc.stater.gateway.annotation.RequestMethod;
import com.mycz.krpcsampleapi.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public String sayHello() {
        return "hello";
    }

    @RequestMapping(method = RequestMethod.GET, path = "/rest/1.0/user/say", name = "测试接口sayGoodBye", description = "描述")
    @Override
    public List<String> sayGoodBye(String name) {
        return Lists.newArrayList(name, "goodbye");
    }

}
