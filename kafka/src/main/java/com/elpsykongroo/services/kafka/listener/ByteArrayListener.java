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

package com.elpsykongroo.services.kafka.listener;

import com.elpsykongroo.base.domain.message.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class ByteArrayListener<K,V> {

    private final String id;
    private final String topic;
    private final String groupId;
    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry;
    private String callback;
    private boolean manualStop;

    public ByteArrayListener(String id, String groupId, String topic, String callback, boolean manualStop) {
        this.id = id;
        this.topic = topic;
        this.callback = callback;
        this.groupId = groupId;
        this.manualStop = manualStop;
    }

    public void stopListen(String id) {
        MessageListenerContainer container = endpointRegistry.getListenerContainer(id);
        if (container != null) {
            if (log.isDebugEnabled()) {
                log.debug("stop consumerId: {}", container.getListenerId());
            }
            if (container.isRunning()) {
                container.stop();
            }
        }
    }

    public String getId() {
        return this.id;
    }

    public String getTopic() {
        return this.topic;
    }

    public String getGroupId() {
        return this.groupId;
    }

    @KafkaListener(id = "#{__listener.id}", topics = "#{__listener.topic}", groupId = "#{__listener.groupId}", idIsGroup = false,
            properties = "value.deserializer:org.apache.kafka.common.serialization.ByteArrayDeserializer")
    public void onMessage(ConsumerRecord<String, byte[]> data, Acknowledgment acknowledgment) {
        RestTemplate restTemplate = new RestTemplate();
        Message message = new Message();
        message.setKey(data.key());
        message.setData(data.value());
        if (data.value().length > 0) {
            String result = restTemplate.postForObject(callback, message, String.class);
            if (Integer.parseInt(result) > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("consumer commit with result:{}", result);
                }
                acknowledgment.acknowledge();
                if (!manualStop) {
                    stopListen(id);
                }
            }
        }
    }
}


