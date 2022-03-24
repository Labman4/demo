package com.elpsykongroo.demo.service.impl;

import com.elpsykongroo.demo.dao.DemoMapper;
import com.elpsykongroo.demo.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DemoServiceImlp implements DemoService {

    @Autowired
    private DemoMapper demoMapper;

    @Override
    public int test() {
        return demoMapper.test();
    }
}
