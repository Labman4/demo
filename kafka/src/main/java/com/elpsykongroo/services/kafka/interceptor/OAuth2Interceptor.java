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
//package com.elpsykongroo.services.kafka.interceptor;
//
//import com.elpsykongroo.base.config.ServiceConfig;
//import feign.RequestInterceptor;
//import feign.RequestTemplate;
//import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
//
//public class OAuth2Interceptor implements RequestInterceptor {
//
//    private ServiceConfig serviceConfig;
//
//    private OAuth2AuthorizedClientManager clientManager;
//
//    public OAuth2Interceptor(OAuth2AuthorizedClientManager clientManager, ServiceConfig serviceConfig) {
//        this.clientManager = clientManager;
//        this.serviceConfig = serviceConfig;
//    }
//
//    @Override
//    public void apply(RequestTemplate requestTemplate) {
//        requestTemplate.header("Authorization", "Bearer " + getAccessToken(clientManager));
//    }
//
//    private String getAccessToken(OAuth2AuthorizedClientManager clientManager) {
//        OAuth2AuthorizeRequest oAuth2AuthorizeRequest = OAuth2AuthorizeRequest
//                .withClientRegistrationId(serviceConfig.getOAuth2().getRegisterId())
//                .principal("kafka")
//                .build();
//        OAuth2AuthorizedClient client = clientManager.authorize(oAuth2AuthorizeRequest);
//        return client.getAccessToken().getTokenValue();
//    }
//}
