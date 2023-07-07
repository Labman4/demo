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


package com.elpsykongroo.storage.listener;

import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.storage.service.ObjectService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

public class ObjectListener<K,V> {
    private final ThreadLocal<Integer> count = new ThreadLocal<>();
    private final String id;
    private final String groupId;
    private final String topic;
    private ObjectService objectService;

    public ObjectListener(String id, String topic, String groupId) {
        this.id = id;
        this.topic = topic;
        this.groupId = groupId;
    }

    public ObjectListener(String id, String topic, String groupId, ObjectService objectService) {
        this.id = id;
        this.topic = topic;
        this.groupId = groupId;
        this.objectService = objectService;
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
    public void onMessage(ConsumerRecord<String, byte[]> data) {
        String[] keys = data.key().split("\\*");
        S3 s3 = new S3();
        s3.setByteData(data.value());
        s3.setPlatform(keys[0]);
        s3.setRegion(keys[1]);
        s3.setBucket(keys[2].split("-")[0]);
        s3.setConsumerId(keys[2]);
        s3.setKey(keys[3]);
        s3.setPartCount(keys[4]);
        s3.setPartNum(keys[5]);
        s3.setUploadId(keys[6]);
        try {
            if (count.get() == null) {
                count.set(0);
            }
            if (count.get() <= 3) {
                objectService.multipartUpload(s3);
            }
        } catch (Exception e) {
            count.set(count.get() + 1);
        }
    }
}


