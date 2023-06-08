/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elpsykongroo.gateway.config;

import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.base.service.AuthService;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.service.SearchService;
import com.elpsykongroo.base.service.StorageService;
import com.elpsykongroo.gateway.interceptor.AuthorizationInterceptor;
import feign.Feign;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.StringDecoder;
import feign.form.spring.SpringFormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
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
    public AuthService authService() {
        return Feign.builder()
                .decoder(new JacksonDecoder())
                .encoder(new JacksonEncoder())
                .requestInterceptor(new AuthorizationInterceptor())
                .target(AuthService.class, serviceConfig.getUrl().getAuth());
    }

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
                .decoder(new JacksonDecoder())
                .encoder(new JacksonEncoder())
                .target(SearchService.class, serviceConfig.getUrl().getEs());
    }

    @Bean
    public StorageService storageService() {
        return Feign.builder()
                .decoder(new StringDecoder())
                .encoder(new SpringFormEncoder(new JacksonEncoder()))
                .target(StorageService.class, serviceConfig.getUrl().getStorage());
    }
}
