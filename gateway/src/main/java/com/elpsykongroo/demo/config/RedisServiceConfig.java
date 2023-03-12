package com.elpsykongroo.demo.config;

import com.elpsykongroo.services.redis.client.RedisService;
import com.elpsykongroo.services.redis.client.impl.RedisServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RedisServiceConfig {

    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Bean
    public RedisService redisService() {
        return  new RedisServiceImpl(serviceConfig.getRedis().getUrl(),  restTemplateBuilder);
    }
}
