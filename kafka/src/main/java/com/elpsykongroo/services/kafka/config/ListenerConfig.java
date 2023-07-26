///*
// * Copyright 2022-2022 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *      https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.elpsykongroo.services.kafka.config;
//
//import com.elpsykongroo.services.kafka.listener.ByteArrayPrototypeListener;
//import com.elpsykongroo.services.kafka.listener.StringPrototypeListener;
//import org.springframework.beans.factory.config.ConfigurableBeanFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Scope;
//
//@Configuration(proxyBeanMethods = false)
//public class ListenerConfig {
//    @Bean
//    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
//    public StringPrototypeListener<?, ?> callbackListener(String id, String groupId, String topic, String callback) {
//        return new StringPrototypeListener<>(id, groupId, topic, callback);
//    }
//
//    @Bean
//    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
//    public ByteArrayPrototypeListener<?, ?> byteArrayListener(String id, String groupId, String topic, String callback, Boolean autoStop) {
//        return new ByteArrayPrototypeListener<>(id, groupId, topic, callback, autoStop);
//    }
//}
