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

import com.elpsykongroo.base.domain.message.OffsetResult;
import com.elpsykongroo.base.domain.message.Send;
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.base.utils.MessageDigestUtils;
import com.elpsykongroo.services.kafka.listener.ObjectListener;
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
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class KafkaServiceImpl implements KafkaService {

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private ApplicationContext ac;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

//    @Autowired
//    private KafkaListenerEndpointRegistry endpointRegistry;

    @Autowired
    private KafkaListenerContainerFactory<?> kafkaListenerContainerFactory;

    private Map<String, ConcurrentMessageListenerContainer<String, Object>> listenerContainer = new ConcurrentHashMap<>();


    @Override
    public void callback(String id, String groupId, String topic, String offset, String callback, Boolean manualStop) {
        if (log.isDebugEnabled()) {
            log.debug("callback id:{}, groupId:{}, topic:{}, offset:{}, callback:{}, manualStop: {}",
                    id, groupId, topic, offset, callback, manualStop);
        }
        String listenerId = String.valueOf(Instant.now().toEpochMilli());
        if (StringUtils.isNotBlank(id)) {
            listenerId = id;
        }
        if (StringUtils.isBlank(groupId)) {
            groupId = listenerId;
        }
        try {
            createContainer(kafkaListenerContainerFactory, topic, id, groupId, callback, manualStop);
//            MessageListenerContainer container = endpointRegistry.getListenerContainer(id);
//            if (container == null) {
//                if (topic.endsWith("-bytes")) {
//                    ac.getBean(ByteArrayPrototypeListener.class, listenerId, groupId, topic, callback, manualStop);
//                } else {
//                    ac.getBean(StringPrototypeListener.class, listenerId, groupId, topic, callback, manualStop);
//                }
//            }
//            MessageListenerContainer listenerContainer = null;
//            while (listenerContainer == null) {
//                listenerContainer = endpointRegistry.getListenerContainer(id);
//                if (StringUtils.isNotBlank(offset)) {
//                    alterOffset(groupId, offset);
//                } else {
//                    alterOffset(groupId, "0");
//                }
//           }
        } catch (IllegalStateException e) {
            if (log.isWarnEnabled()) {
                log.warn("already on listen:{}", e.getMessage());
            }
        } catch (BeansException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String stopListen(String ids) {
        String[] consumerIds = ids.split(", ");
//        Collection<MessageListenerContainer> containers = endpointRegistry.getAllListenerContainers();
        for (String consumer : consumerIds) {
            if (log.isDebugEnabled()) {
                log.debug("current groupId:{}, listener size:{}", consumer, listenerContainer.size());
            }
            if (listenerContainer.containsKey(consumer)) {
                ConcurrentMessageListenerContainer<String, Object> listener = listenerContainer.get(consumer);
                if (listener != null && listener.isRunning()) {
                    if (log.isDebugEnabled()) {
                        log.debug("stop consumerId: {}", listener.getListenerId());
                    }
                    listener.stop();
                    while (listener.isRunning()) {
                        if (log.isDebugEnabled()) {
                            log.debug("wait listener stop");
                        }
                        if (!listener.isRunning()) {
                            listenerContainer.remove(consumer);
                        }
                    }
                }
            }
//            MessageListenerContainer container = endpointRegistry.getListenerContainer(consumer);
//            endpointRegistry.unregisterListenerContainer(consumer);
//            if (container != null) {
//                if (log.isDebugEnabled()) {
//                    log.debug("stop consumerId: {}", container.getListenerId());
//                }
//                if (container.isRunning()) {
//                    container.stop();
//                }
//            }
        }
        return listenersState(ids);
    }

    @Override
    public String listenersState(String ids) {
        boolean flag = false;
        String[] consumerIds = ids.split(", ");
        for (String consumer : consumerIds) {
            if (listenerContainer.containsKey(consumer)) {
                ConcurrentMessageListenerContainer<String, Object> listener = listenerContainer.get(consumer);
                if (listener != null && listener.isRunning()) {
                    flag = true;
                    if (log.isDebugEnabled()) {
                        log.debug("listener assign:{}", listener.getAssignedPartitions());
                    }
//                    if (listener.getAssignedPartitions().size() > 0 ) {
//                        flag = true;
//                    }
                }
            }
//            MessageListenerContainer container = endpointRegistry.getListenerContainer(consumer);
//            if (container != null) {
//                if (container.isRunning()) {
//                    if (log.isDebugEnabled()) {
//                        log.debug("listener assign:{}", container.getAssignedPartitions());
//                    }
//                    if (container.getAssignedPartitions().size() > 0) {
//                        flag = true;
//                    }
//                }
//            }
        }
        if (log.isDebugEnabled()) {
            log.debug("listener state:{}", flag);
        }
        return String.valueOf(flag);
    }

    @Override
    public void deleteTopic(String topic, String groupId) {
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        try {
            try {
                String state = listenersState(groupId);
                listenerContainer.get(groupId);
                if ("true".equals(state)) {
                    waitStopListen(groupId);
                }
                adminClient.deleteConsumerGroups(Collections.singleton(groupId)).all().get();
                if ("false".equals(listenersState(groupId))) {
                    adminClient.deleteTopics(Collections.singleton(topic));
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("deleteTopic error:{}", e.getMessage());
                }
            }
        } finally {
            adminClient.close();
        }
    }

    @Override
    public String getOffset(String consumerGroupId) {
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        try {
            ListConsumerGroupOffsetsResult result = adminClient.listConsumerGroupOffsets(consumerGroupId);
            Map<TopicPartition, OffsetAndMetadata> offsets = result.partitionsToOffsetAndMetadata().get();
            List<OffsetResult> offsetResult = new ArrayList<>();
            for (TopicPartition partition: offsets.keySet()) {
                OffsetResult offset = new OffsetResult();
                offset.setOffset(offsets.get(partition).offset());
                offset.setTopicPartition(partition.partition());
                offsetResult.add(offset);
            }
            return JsonUtils.toJson(offsetResult);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("get offset error: {}", e.getMessage());
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
                log.debug("manual alert offset:{}, groupId", offset, consumerGroupId);
            }
            Map<String, Map<TopicPartition, OffsetAndMetadata>> result = adminClient.listConsumerGroupOffsets(consumerGroupId).all().get();
            Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
            for (String key : result.keySet()) {
                for (TopicPartition partition : result.get(key).keySet()) {
                    if (log.isDebugEnabled()) {
                        log.debug("partition: {}, topic:{}, leaderEpoch: {}, offset before:{}",
                                partition.partition(),
                                partition.topic(),
                                result.get(key).get(partition).leaderEpoch().isPresent(),
                                result.get(key).get(partition).offset());
                    }
                    if (StringUtils.isBlank(offset)) {
                        if (result.get(key).get(partition) != null && result.get(key).get(partition).offset() > 0) {
                            offsets.put(partition, new OffsetAndMetadata(result.get(key).get(partition).offset()-1));
                        } else {
                            offsets.put(partition, new OffsetAndMetadata(0));
                        }
                    } else {
                        offsets.put(partition, new OffsetAndMetadata(Integer.parseInt(offset)));
                    }
                }
            }
            waitStopListen(consumerGroupId);
            if ("false".equals(listenersState(consumerGroupId))) {
                if (log.isDebugEnabled()) {
                    log.debug("alert with offset:{}", offsets);
                }
                adminClient.alterConsumerGroupOffsets(consumerGroupId, offsets).all().get();
            }
            if (log.isDebugEnabled()) {
                log.debug("alert offset after:{}", getOffset(consumerGroupId));
            }

        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("alert offset error:{}", e);
            }
        } finally {
            adminClient.close();
        }
    }

    private void waitStopListen(String consumerGroupId) {
        stopListen(consumerGroupId);
        while("true".equals(listenersState(consumerGroupId))) {
            if (log.isDebugEnabled()) {
                log.debug("wait listener stop");
            }
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

    private void createContainer(KafkaListenerContainerFactory<?> factory, String topic, String id, String group, String callback, boolean manualStop) {
        ConcurrentMessageListenerContainer<String, Object> container = (ConcurrentMessageListenerContainer<String, Object>) factory.createContainer(topic);
        listenerContainer.putIfAbsent(group, container);
        container.getContainerProperties().setMessageListener(new ObjectListener(callback, manualStop, container));
        container.getContainerProperties().setGroupId(group);
        container.setBeanName(group);
        container.setMainListenerId(id);
        container.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        Properties properties = new Properties();
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.DelegatingDeserializer");
        properties.put("allow.auto.create.topics", false);
        properties.put("enable.auto.commit", "false");
        properties.put("auto.offset.reset", "earliest");
        container.getContainerProperties().setKafkaConsumerProperties(properties);
        container.start();
    }
}
