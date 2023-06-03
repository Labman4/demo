package com.elpsykongroo.gateway.config;


import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.service.SearchService;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FeginConfig {
//    @Bean
//    public Contract useFeignAnnotations() {
//        return new Contract.Default();
//    }

    @Autowired
    private ServiceConfig serviceConfig;

    @Bean
    public RedisService redisService() {
        return Feign.builder()
                .decoder(new Decoder.Default())
                .encoder(new Encoder.Default())
                .target(RedisService.class, serviceConfig.getUrl().getRedis());
    }

    @Bean
    public SearchService searchService() {
        return Feign.builder()
                .decoder(new Decoder.Default())
                .encoder(new Encoder.Default())
                .target(SearchService.class, serviceConfig.getUrl().getEs());
    }

//    @Bean
//    public StorageService storageService() {
//        return Feign.builder()
//                .decoder(new Decoder.Default())
//                .encoder(new Encoder.Default())
//                .target(StorageService.class, serviceConfig.getUrl().getStorage());
//    }
}
