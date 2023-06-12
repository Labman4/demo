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

import com.elpsykongroo.base.service.GatewayService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

public class RedisSubscriber implements MessageListener {
    private GatewayService gatewayService;

    public RedisSubscriber(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
         gatewayService.receiveMessage(message.toString());
    }
}
