package com.elpsykongroo.services.redis.server.service;

import org.springframework.stereotype.Service;

@Service
public interface RedisService {
    void setCache(String key, String vaule, String minutes);

    String getCache(String key);
}
