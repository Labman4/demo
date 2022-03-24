package com.elpsykongroo.demo.controller;

import com.elpsykongroo.demo.service.DemoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api")
public class DemoContronller {
    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DemoService demoService;

    @GetMapping("")
    public String access() {

        System.out.println(demoService.test());
        redisTemplate.getClientList();
        return "0";
    }
}
