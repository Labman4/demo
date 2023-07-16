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

package com.elpsykongroo.services.kafka.service.impl;

import com.elpsykongroo.base.domain.message.Send;
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.base.utils.MessageDigestUtils;
import com.elpsykongroo.services.kafka.listener.ByteArrayListener;
import com.elpsykongroo.services.kafka.listener.StringListener;
import com.elpsykongroo.services.kafka.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class KafkaServiceImpl implements KafkaService {

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private ApplicationContext ac;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry;

    @Override
    public void callback(String id, String groupId, String topic, String callback, Boolean manualStop) {
        if (log.isDebugEnabled()) {
            log.debug("id:{}, groupId:{}", id, groupId);
        }
        String listenerId = String.valueOf(Instant.now().toEpochMilli());
        if (StringUtils.isNotBlank(id)) {
            listenerId = id;
        }
        if (StringUtils.isBlank(groupId)) {
            groupId = listenerId;
        }
        try {
            MessageListenerContainer container = endpointRegistry.getListenerContainer(id);
            if (container == null) {
                alterOffset(groupId, "0");
                if (topic.endsWith("-bytes")) {
                    ac.getBean(ByteArrayListener.class, listenerId, groupId, topic, callback, manualStop);
                } else {
                    ac.getBean(StringListener.class, listenerId, groupId, topic, callback, manualStop);
                }
            }
        } catch (BeansException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            if (log.isWarnEnabled()) {
                log.warn("already on listen:{}", e.getMessage());
            }
        }
    }

    @Override
    public void stopListen(String ids) {
        String[] consumerIds = ids.split(", ");
        for (String consumer : consumerIds) {
            MessageListenerContainer container = endpointRegistry.getListenerContainer(consumer);
            if (container != null) {
                if (log.isDebugEnabled()) {
                    log.debug("stop consumerId: {}", container.getListenerId());
                }
                if (container.isRunning()) {
                    container.stop();
                }
            }
        }
    }

    @Override
    public String listenersState(String ids) {
        boolean flag = false;
        String[] consumerIds = ids.split(", ");
        for (String consumer : consumerIds) {
            MessageListenerContainer container = endpointRegistry.getListenerContainer(consumer);
            if (container != null) {
                if (container.isRunning()) {
                    flag = true;
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("listener state:{}", flag);
        }
        return String.valueOf(flag);
    }

    @Override
    public void deleteTopic(String topic) {
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        try {
            adminClient.deleteTopics(Collections.singleton(topic));
        } finally {
            adminClient.close();
        }
    }

    public String getOffset(String consumerGroupId) {
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        try {
            ListConsumerGroupOffsetsResult result = adminClient.listConsumerGroupOffsets(consumerGroupId);
            Map<TopicPartition, OffsetAndMetadata> offsets = result.partitionsToOffsetAndMetadata().get();
            return JsonUtils.toJson(offsets);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("alert offset error: {}", e.getMessage());
            }
            return "";
        } finally {
            adminClient.close();
        }
    }

    @Override
    public synchronized void alterOffset(String consumerGroupId, String offset) {
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        try {
            if (log.isDebugEnabled()) {
                log.debug("manual alert offset:{}", offset);
            }
            ListConsumerGroupOffsetsResult result = adminClient.listConsumerGroupOffsets(consumerGroupId);
            Map<TopicPartition, OffsetAndMetadata> offsets = result.partitionsToOffsetAndMetadata().get();
            for (TopicPartition partition : offsets.keySet()) {
                if (log.isDebugEnabled()) {
                    log.debug("partition: {}, topic:{}, leaderEpoch: {}, offset before:{}",
                            partition.partition(),
                            partition.topic(),
                            offsets.get(partition).leaderEpoch().get(),
                            offsets.get(partition).offset());
                }

                if (Integer.parseInt(offset) > 0) {
                    offsets.put(partition, new OffsetAndMetadata(Integer.parseInt(offset)));
                } else {
                    if (offsets.get(partition).offset() > 0) {
                        offsets.put(partition, new OffsetAndMetadata(offsets.get(partition).offset()-1));
                    } else {
                        offsets.put(partition, new OffsetAndMetadata(0));
                    }
                }
            }
            adminClient.alterConsumerGroupOffsets(consumerGroupId, offsets);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("alert offset error: {}", e.getMessage());
            }
        } finally {
            adminClient.close();
        }
    }

    @Override
    public String send(Send send) {
        CompletableFuture<SendResult<String, Object>> result;
        try {
            if (send.getData() != null) {
                String dataSha256 = MessageDigestUtils.sha256(send.getData());
                if (dataSha256.equals(send.getSha256())) {
                    result = kafkaTemplate.send(send.getTopic(), send.getKey(), send.getData());
                    String sha256 = MessageDigestUtils.sha256((byte[]) result.get().getProducerRecord().value());
                    if (send.getSha256().equals(sha256)) {
                        return "1";
                    }
                }
            } else {
                String dataSha256 = MessageDigestUtils.sha256(send.getMessage());
                if (dataSha256.equals(send.getSha256())) {
                    result = kafkaTemplate.send(send.getTopic(), send.getKey(), send.getMessage());
                    String sha256 = MessageDigestUtils.sha256((String) result.get().getProducerRecord().value());
                    if (send.getSha256().equals(sha256)) {
                        return "1";
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "0";
    }
}
