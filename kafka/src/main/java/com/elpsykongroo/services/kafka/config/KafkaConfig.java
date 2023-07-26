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

import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.converter.JsonMessageConverter;
import org.springframework.kafka.support.serializer.DelegatingDeserializer;
import org.springframework.kafka.support.serializer.DelegatingSerializer;

import java.util.HashMap;
import java.util.Map;

 @Configuration(proxyBeanMethods = false)
public class KafkaConfig {

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.putAll(kafkaAdmin.getConfigurationProperties());
        config.put(DelegatingSerializer.VALUE_SERIALIZATION_SELECTOR_CONFIG,
                "byte[]:" + ByteArraySerializer.class.getName()
                + ", string: " + StringSerializer.class.getName());
        return new DefaultKafkaProducerFactory<>(config, new StringSerializer(), new DelegatingSerializer());
//        return new DefaultKafkaProducerFactory<>(kafkaAdmin.getConfigurationProperties(), new StringSerializer(),
//                new DelegatingByTypeSerializer(Map.of(
//                        byte[].class, new ByteArraySerializer(),
//                        Bytes.class, new BytesSerializer(),
//                        String.class, new StringSerializer())));
////        config.put(DelegatingByTopicSerializer.VALUE_SERIALIZATION_TOPIC_CONFIG,
////                "-bytes$:" + ByteArraySerializer.class.getName() +
////                        ", -string$:" + StringDeserializer.class.getName());
////       return new DefaultKafkaProducerFactory<>(config, new StringSerializer(),
////                new DelegatingByTopicSerializer(Map.of(
////                        Pattern.compile("-bytes$"), new ByteArraySerializer(),
////                        Pattern.compile("-string$"), new StringSerializer()),
////                new JsonSerializer<Object>()));
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.putAll(kafkaAdmin.getConfigurationProperties());
//        config.put(DelegatingByTopicDeserializer.VALUE_SERIALIZATION_TOPIC_CONFIG,
//                "-bytes$:" + ByteArrayDeserializer.class.getName() +
//                ", -string$:" + StringDeserializer.class.getName());
        return new DefaultKafkaConsumerFactory<>(config,  new StringDeserializer(),
                new DelegatingDeserializer(Map.of(
                        byte[].class.getName(), new ByteArrayDeserializer(),
                        Bytes.class.getName(), new BytesDeserializer(),
                        String.class.getName(), new StringDeserializer())));
//        return new DefaultKafkaConsumerFactory<>(config,  new StringDeserializer(),
//                new DelegatingByTopicDeserializer(Map.of(
//                        Pattern.compile("-bytes$"), new ByteArrayDeserializer(),
//                        Pattern.compile("-string$"), new StringDeserializer()),
//                        new JsonDeserializer<Object>()));
    }

    @Bean
    public KafkaListenerContainerFactory<?> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setRecordMessageConverter(new JsonMessageConverter());
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
