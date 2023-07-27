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

package com.elpsykongroo.services.redis.service.impl;

import com.elpsykongroo.base.config.ServiceConfig;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestTemplate;

public class RedisSubscriber implements MessageListener {

    private ThreadLocal<Integer> count = new ThreadLocal<>();

    private ServiceConfig serviceConfig;

    private OAuth2AuthorizedClientManager clientManager;

    private String callback;

    public RedisSubscriber(String callback, ServiceConfig serviceConfig, OAuth2AuthorizedClientManager clientManager) {
        this.callback = callback;
        this.serviceConfig = serviceConfig;
        this.clientManager = clientManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + getAccessToken(clientManager));
        com.elpsykongroo.base.domain.message.Message msg = new com.elpsykongroo.base.domain.message.Message();
        msg.setValue(message.toString());
        RequestEntity requestEntity = RequestEntity.post(callback).headers(headers).body(msg);
        count.set(0);
        while (restTemplate.exchange(requestEntity, String.class).getStatusCode().is2xxSuccessful()) {
            if (count.get() < 3 ) {
                restTemplate.exchange(requestEntity, String.class);
            }
            count.set(count.get() + 1);
        }
    }

    private String getAccessToken(OAuth2AuthorizedClientManager clientManager) {
        OAuth2AuthorizeRequest oAuth2AuthorizeRequest = OAuth2AuthorizeRequest
                .withClientRegistrationId(serviceConfig.getOAuth2().getRegisterId())
                .principal("redis")
                .build();
        OAuth2AuthorizedClient client = clientManager.authorize(oAuth2AuthorizeRequest);
        return client.getAccessToken().getTokenValue();
    }
}
