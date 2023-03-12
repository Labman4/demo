package com.elpsykongroo.services.redis.server.service.impl;

import com.elpsykongroo.services.redis.server.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


@Service
public class RedisServiceImpl implements RedisService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Override
    public void setCache(String key, String value) {
           redisTemplate.opsForValue().set(key, value);
    }

    @Override
    public String getCache(String key) {
        Object result = redisTemplate.opsForValue().get(key);
        return result == null ? "" : result.toString();
    }
}
