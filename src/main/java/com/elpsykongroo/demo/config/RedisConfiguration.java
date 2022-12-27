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

package com.elpsykongroo.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfiguration {
//	@Value("${REDIS_IP}")
//	private String URL;
//
//	@Value("${REDIS_PORT}")
//	private Integer PORT;
//
//	@Value("${REDIS_PASSWORD}")
//	private String PASSWORD;

//	@Bean
//	public JedisConnectionFactory redisConnectionFactory() {
//
//		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(URL, PORT);
//		config.setPassword(PASSWORD);
//		return new JedisConnectionFactory(config);
//	}

//	@Bean
//	public ReactiveRedisConnectionFactory lettuceConnectionFactory() {
//
//		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
//				.commandTimeout(Duration.ofSeconds(2))
//				.shutdownTimeout(Duration.ZERO)
//				.build();
//
//		return new LettuceConnectionFactory(new RedisStandaloneConfiguration(URL, 6379), clientConfig);
//	}

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(factory);
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		redisTemplate.setKeySerializer(stringRedisSerializer);
		redisTemplate.setHashKeySerializer(stringRedisSerializer);
		redisTemplate.setValueSerializer(stringRedisSerializer);
		redisTemplate.afterPropertiesSet();
		return redisTemplate;
	}
}
