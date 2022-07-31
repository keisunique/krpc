package com.mycz.dubboprovider.impl;

import com.mycz.krpcsampleapi.OrderService;
import org.springframework.stereotype.Service;

@Service
public class OrderServiceImpl implements OrderService {

    @Override
    public int total() {
        return 10000000;
    }

}
