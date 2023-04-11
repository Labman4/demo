package com.elpsykongroo.services.redis.client.impl;

import com.elpsykongroo.services.redis.client.RedisService;
import com.elpsykongroo.services.redis.client.dto.KV;
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

    public RedisServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
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
