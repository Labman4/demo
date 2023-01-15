package com.elpsykongroo.demo.controller;

import com.elpsykongroo.services.redis.RedisService;
import com.elpsykongroo.services.redis.dto.KV;
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
        redisService.set(kv);
    }

    @PostMapping("get")
    public String patchCache(@RequestBody KV kv) {
        return redisService.get(kv);
    }
}
