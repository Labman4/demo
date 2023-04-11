package com.elpsykongroo.services.redis.server.controller;

import com.elpsykongroo.services.redis.server.entity.KV;
import com.elpsykongroo.services.redis.server.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("redis")
public class RedisController {
    @Autowired
    private RedisService redisService;

    @PostMapping("set")
    public void set(@RequestBody KV kv) {
        redisService.setCache(kv.getKey(), kv.getValue(), kv.getTime());
    }

    @GetMapping("get")
    public String get(@RequestParam("key") String key) {
        return redisService.getCache(key);
    }
}
