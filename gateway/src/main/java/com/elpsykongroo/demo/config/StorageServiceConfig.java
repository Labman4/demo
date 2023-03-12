package com.elpsykongroo.demo.config;

import com.elpsykongroo.storage.client.StorageService;
import com.elpsykongroo.storage.client.impl.StorageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)

public class StorageServiceConfig {
    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Bean
    public StorageService storageService() {
        return  new StorageServiceImpl(serviceConfig.getStorage().getUrl(),  restTemplateBuilder);
    }
}
