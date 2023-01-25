package com.elpsykongroo.services.redis.impl;

import com.elpsykongroo.services.redis.RedisService;
import com.elpsykongroo.services.redis.dto.KV;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RedisServiceImpl implements RedisService {
    private RestTemplate restTemplate;
    private String serverUrl = "http://localhost:8379";
    private String servicePrefix =  "/redis";

    public RedisServiceImpl(String serverUrl, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.serverUrl = serverUrl;
    }

    @Override
    public void set(KV kv) {
        restTemplate.postForObject(serverUrl + servicePrefix + "/set", kv, String.class);
    }

    @Override
    public String get(String key) {
        String result = restTemplate.getForEntity(serverUrl + servicePrefix + "/get?key=" + key, String.class).getBody();
        return result == null ? "" : result;
    }
}
