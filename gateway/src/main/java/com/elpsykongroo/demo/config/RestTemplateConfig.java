 package com.elpsykongroo.demo.config;


 import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
 import org.springframework.boot.web.client.RestTemplateBuilder;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;

 import java.time.Duration;

 @Configuration(proxyBeanMethods = false)
 public class RestTemplateConfig {
    
      @Bean
      public RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer) {
          return configurer.configure(new RestTemplateBuilder())
                  .setConnectTimeout(Duration.ofSeconds(10))
                  .setReadTimeout(Duration.ofSeconds(20)).detectRequestFactory(true);
      }

 }
