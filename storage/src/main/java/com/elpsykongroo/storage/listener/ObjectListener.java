package com.elpsykongroo.storage.listener;

import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.storage.service.ObjectService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;

public class ObjectListener<K,V> {
    private final String id;
    private final String topic;
    private ObjectService objectService;

    public ObjectListener(String id, String topic) {
        this.id = id;
        this.topic = topic;
    }

    public ObjectListener(String id, String topic, ObjectService objectService) {
        this.id = id;
        this.topic = topic;
        this.objectService = objectService;
    }

    public String getId() {
        return this.id;
    }

    public String getTopic() {
        return this.topic;
    }

    @KafkaListener(id = "#{__listener.id}", topics = "#{__listener.topic}",
            properties = "value.deserializer:org.apache.kafka.common.serialization.ByteArrayDeserializer")
    public void onMessage(ConsumerRecord<String, byte[]> data) {
        String[] keys = data.key().split("\\*");
        S3 s3 = new S3();
        s3.setByteData(data.value());
        s3.setBucket(keys[0].split("-")[0]);
        s3.setConsumerId(keys[0]);
        s3.setKey(keys[1]);
        s3.setPartCount(keys[2]);
        s3.setPartNum(keys[3]);
        s3.setUploadId(keys[4]);
        try {
            objectService.multipartUpload(s3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}


