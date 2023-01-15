package com.elpsykongroo.demo.config;

import com.elpsykongroo.services.redis.RedisService;
import com.elpsykongroo.services.redis.impl.RedisServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
public class RedisConfig {

    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Bean
    public RedisService redisService () {
        return  new RedisServiceImpl(serviceConfig.getRedis().getUrl(), restTemplate);
    }
}
