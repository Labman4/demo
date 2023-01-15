package com.elpsykongroo.services.redis.impl;

import com.elpsykongroo.services.redis.RedisService;
import com.elpsykongroo.services.redis.dto.KV;
import org.springframework.web.client.RestTemplate;

public class RedisServiceImpl implements RedisService {
    private String serverUrl;
    private String servicePrefix =  "/redis";

    public RedisServiceImpl(String serverUrl, RestTemplate restTemplate) {
        this.serverUrl = serverUrl;
        this.restTemplate = restTemplate;
    }

    private RestTemplate restTemplate;

    @Override
    public void set(KV kv) {
        restTemplate.postForObject(serverUrl + servicePrefix + "/set", kv, String.class);
    }

    @Override
    public String get(KV kv) {
        String result = restTemplate.postForEntity(serverUrl + servicePrefix + "/get", kv, String.class).getBody();
        return result == null ? "" : result;
    }
}
