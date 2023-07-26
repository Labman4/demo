///*
// * Copyright 2022-2022 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.elpsykongroo.services.redis.config;
//
//import com.elpsykongroo.base.config.ServiceConfig;
//import com.elpsykongroo.base.service.GatewayService;
//import com.elpsykongroo.services.redis.interceptor.OAuth2Interceptor;
//import feign.Feign;
//import feign.codec.Decoder;
//import feign.codec.Encoder;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
//
//
//@Configuration(proxyBeanMethods = false)
//public class FeignConfig {
//
//    @Autowired
//    private ServiceConfig serviceConfig;
//
//    @Autowired
//    private OAuth2AuthorizedClientManager clientManager;
//
//    @Bean
//    public GatewayService gatewayService() {
//        return Feign.builder()
//                .decoder(new Decoder.Default())
//                .encoder(new Encoder.Default())
//                .requestInterceptor(new OAuth2Interceptor(clientManager, serviceConfig))
//                .target(GatewayService.class, serviceConfig.getUrl().getGateway());
//    }
//
//}
