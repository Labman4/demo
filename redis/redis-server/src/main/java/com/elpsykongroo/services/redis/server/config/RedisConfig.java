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

import com.elpsykongroo.base.config.ServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisClusterNode;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisNode;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.vault.annotation.VaultPropertySource;

import java.util.Collections;

@VaultPropertySource(value = "${SECRETS_DATA_PATH:database/creds/redis-cluster}", renewal = VaultPropertySource.Renewal.RENEW)
@Configuration(proxyBeanMethods = false)
public class RedisConfig {
    @Autowired
    private ServiceConfig serviceConfig;

    @Value("${username}")
    private String username;

    @Value("${password}")
    private String password;

     @Bean
     public JedisConnectionFactory redisConnectionFactory() {
         if ("single".equals(serviceConfig.getRedis().getType())) {
              RedisStandaloneConfiguration singleConfig = new RedisStandaloneConfiguration();
              singleConfig.setHostName(serviceConfig.getRedis().getHost());
              singleConfig.setPort(serviceConfig.getRedis().getPort());
//              singleConfig.setUsername(username);
//              singleConfig.setPassword(password);
           return new JedisConnectionFactory(singleConfig);
         } else {
             RedisClusterConfiguration config = new RedisClusterConfiguration();
             config.setUsername(username);
             config.setPassword(password);
             RedisNode redisNode = new RedisClusterNode(serviceConfig.getRedis().getHost(), serviceConfig.getRedis().getPort());
             config.setClusterNodes(Collections.singletonList(redisNode));
             return new JedisConnectionFactory(config);
         }
    }
    //}
     @Bean
     public <K, V> RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
         RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
         redisTemplate.setConnectionFactory(factory);
         StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
         redisTemplate.setKeySerializer(stringRedisSerializer);
         redisTemplate.setHashKeySerializer(stringRedisSerializer);
         redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
         redisTemplate.setValueSerializer(stringRedisSerializer);
         redisTemplate.afterPropertiesSet();
         return redisTemplate;
     }
}
