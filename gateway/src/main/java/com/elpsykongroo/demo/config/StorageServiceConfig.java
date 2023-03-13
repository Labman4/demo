package com.elpsykongroo.demo.config;

import com.elpsykongroo.storage.client.StorageService;
import com.elpsykongroo.storage.client.impl.StorageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)

public class StorageServiceConfig {
    @Autowired
    private ServiceConfig serviceConfig;

    @Bean
    public StorageService storageService(RestTemplateBuilderConfigurer configurer) {
        RestTemplateBuilder restTemplateBuilder = configurer.configure(new RestTemplateBuilder())
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30)).detectRequestFactory(true);
        return  new StorageServiceImpl(serviceConfig.getStorage().getUrl(),  restTemplateBuilder);
    }
}
