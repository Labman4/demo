package com.elpsykongroo.demo.config;

import com.elpsykongroo.auth.client.AuthService;
import com.elpsykongroo.auth.client.impl.AuthServiceImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)

public class AuthServiceConfig {
    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Bean
    public AuthService authService() {
        return  new AuthServiceImpl(serviceConfig.getAuth().getUrl(),  restTemplateBuilder);
    }
}
