package com.elpsykongroo.services.redis.server.service.impl;

import com.elpsykongroo.services.redis.server.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;


@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Override
    public void setCache(String key, String value, String minutes) {
        log.debug("set cache with k-v: {} -> {}", key, value);
        if (StringUtils.isNotBlank(minutes)) {
            redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(Integer.parseInt(minutes)));
        } else {
            redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));
        }
    }

    @Override
    public String getCache(String key) {
        log.debug("get cache with k: {}", key);
        Object result = redisTemplate.opsForValue().get(key);
        log.debug("get cache result: {} --> {}", key, result == null ? "" : result.toString());
        return result == null ? "" : result.toString();
    }
}
