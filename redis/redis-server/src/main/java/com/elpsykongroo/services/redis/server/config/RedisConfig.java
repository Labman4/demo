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

package com.elpsykongroo.services.redis.server.config;

import org.springframework.context.annotation.Configuration;

 @Configuration(proxyBeanMethods = false)
// @EnableRedisRepositories("com.elpsykongroo.services.repo")
 public class RedisConfig {

    // @Bean
    // public JedisConnectionFactory redisConnectionFactory() {      
        // RedisStandaloneConfiguration singleConfig = new RedisStandaloneConfiguration();
        //   singleConfig.setHostName(host);
        //   singleConfig.setPassword(pass);
        // return new JedisConnectionFactory(singleConfig);
    //}
//     @Bean
//     public <K, V> RedisTemplate<String, Object> redisTemplate(JedisConnectionFactory factory) {
//         RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
//         redisTemplate.setConnectionFactory(factory);
//         StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
//         redisTemplate.setKeySerializer(stringRedisSerializer);
//         redisTemplate.setHashKeySerializer(stringRedisSerializer);
//         redisTemplate.setValueSerializer(stringRedisSerializer);
//         redisTemplate.afterPropertiesSet();
//         return redisTemplate;
//     }
}
