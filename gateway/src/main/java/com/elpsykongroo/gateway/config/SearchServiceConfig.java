package com.elpsykongroo.gateway.config;

import com.elpsykongroo.services.elasticsearch.client.SearchService;
import com.elpsykongroo.services.elasticsearch.client.impl.SearchServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SearchServiceConfig {
    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Bean
    public SearchService searchService() {
        return  new SearchServiceImpl(serviceConfig.getEs().getUrl(),  restTemplateBuilder);
    }
}
