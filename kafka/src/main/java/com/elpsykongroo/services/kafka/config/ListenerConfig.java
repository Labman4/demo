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

package com.elpsykongroo.services.kafka.config;

import com.elpsykongroo.services.kafka.listener.ByteArrayListener;
import com.elpsykongroo.services.kafka.listener.StringListener;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class ListenerConfig {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public StringListener<?, ?> callbackListener(String id, String groupId, String topic, String callback) {
        return new StringListener<>(id, groupId, topic, callback);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ByteArrayListener<?, ?> byteArrayListener(String id, String groupId, String topic, String callback, Boolean autoStop) {
        return new ByteArrayListener<>(id, groupId, topic, callback, autoStop);
    }
}
