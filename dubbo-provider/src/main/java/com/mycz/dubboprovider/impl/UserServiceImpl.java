package com.mycz.dubboprovider.impl;

import com.google.common.collect.Lists;
import com.mycz.krpcsampleapi.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public String sayHello() {
        return "hello";
    }

    @Override
    public List<String> sayGoodBye(String name) {
        return Lists.newArrayList(name, "goodbye");
    }
}
