package com.elpsykongroo.services.redis.controller;

import com.elpsykongroo.services.redis.entity.KV;
import com.elpsykongroo.services.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("redis")
public class RedisController {
    @Autowired
    private RedisService redisService;

    @PostMapping("set")
    public void addCache(@RequestBody KV kv) {
        redisService.setCache(kv.getKey(), kv.getValue());
    }

    @PostMapping("get")
    public String patchCache(@RequestBody KV kv) {
        return redisService.getCache(kv.getKey());
    }
}
