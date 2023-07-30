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
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.web.client.RestTemplate;

@Slf4j
public class ObjectListener implements AcknowledgingMessageListener<String, Object> {

    private String callback;

    private boolean manualStop;

    private ConcurrentMessageListenerContainer<String, Object> container;

    public ObjectListener(String callback, boolean manualStop, ConcurrentMessageListenerContainer<String, Object> container) {
        this.callback = callback;
        this.manualStop = manualStop;
        this.container = container;
    }

    @Override
    public void onMessage(ConsumerRecord<String, Object> data, Acknowledgment acknowledgment) {
        RestTemplate restTemplate = new RestTemplate();
        Message message = new Message();
        message.setKey(data.key());
        if (data.value() instanceof byte[]) {
            message.setData((byte[]) data.value());
        } else {
            message.setValue((String) data.value());
        }
        String result = restTemplate.postForObject(callback, message, String.class);
        if (Integer.parseInt(result) > 0) {
            if (log.isDebugEnabled()) {
                log.debug("consumer commit with result:{}", result);
            }
            acknowledgment.acknowledge();
            if (!manualStop) {
                if (container != null && container.isRunning()) {
                    if (log.isDebugEnabled()) {
                        log.debug("stop listener");
                    }
                }
            }
        }
    }
}
