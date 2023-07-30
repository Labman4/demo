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

package com.elpsykongroo.services.redis.service.impl;

import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.base.utils.EncryptUtils;
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.services.redis.entity.MsgPack;
import com.elpsykongroo.services.redis.utils.convert.TimestampExtensionModule;
import com.elpsykongroo.services.redis.service.RedisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Base64;

@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    private OAuth2AuthorizedClientManager clientManager;

    @Override
    public void setCache(String key, String value, String minutes) {
        if (StringUtils.isBlank(key)) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("set cache with k-v: {} -> {}", key, value);
        }
        if (StringUtils.isNotBlank(minutes)) {
            redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(Integer.parseInt(minutes)));
        } else {
            redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(30));
        }
    }

    @Override
    public String getCache(String key) {
        if (StringUtils.isBlank(key)) {
            return "";
        }
        if (log.isDebugEnabled()) {
            log.debug("get cache with k: {}", key);
        }
        Object result = redisTemplate.opsForValue().get(key);
        if (log.isDebugEnabled()) {
            log.debug("get cache result: {} --> {}", key, result == null ? "" : result.toString());
        }
        return result == null ? "" : result.toString();
    }

//    private String getType(String key) {
//        String type = redisTemplate.execute((connection) -> {
//            return connection.type(key.getBytes());
//        }, true).toString();
//        log.debug("data type:{}", type);
//        return type;
//    }

    @Override
    public String getToken(String key){
        if (StringUtils.isBlank(key)) {
            return "";
        }
        String[] ticket = key.split("\\.");
        if (ticket.length < 2) {
            return "";
        }
        MsgPack obj;
        try {
            byte[] bytes = redisTemplate.execute((RedisCallback<byte[]>) connection -> {
                return connection.get(ticket[0].getBytes());
            });
            byte[] secret = Base64.getUrlDecoder().decode(ticket[1]);
            byte[] plainText = EncryptUtils.decryptAsByte(bytes, secret);
//        LZ4Factory factory = LZ4Factory.fastestInstance();
//        LZ4FastDecompressor decompressor = factory.fastDecompressor();
//        LZ4DecompressorWithLength decompressorWithLength = new LZ4DecompressorWithLength(decompressor);
//        byte[] restored = decompressorWithLength.decompress(plainText);
//        System.out.println(new String(restored));

//        ByteArrayInputStream compressedInput = new ByteArrayInputStream(plainText);
//        byte[] restored = new byte[plainText.length * 100];
//        LZ4FrameInputStream inStream = new LZ4FrameInputStream(compressedInput);
//        inStream.read(restored);
//        inStream.close();
//        System.out.println(restored.length);
            MessageUnpacker unPacker = MessagePack.newDefaultUnpacker(plainText);
            ObjectMapper mapper = new ObjectMapper(new MessagePackFactory())
                    .registerModule(new JavaTimeModule())
                    .registerModule(TimestampExtensionModule.INSTANCE);
            obj = mapper.readValue(plainText, MsgPack.class);
            unPacker.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return JsonUtils.toJson(obj);
    }

    @Override
    public void publish(String topic, String message, String callback) {
        RedisSubscriber redisSubscriber = new RedisSubscriber(callback, serviceConfig, clientManager);
        ChannelTopic channelTopic = new ChannelTopic(topic);
        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
        redisMessageListenerContainer.addMessageListener(redisSubscriber, channelTopic);
        redisMessageListenerContainer.afterPropertiesSet();
        redisMessageListenerContainer.start();
        redisTemplate.convertAndSend(channelTopic.getTopic(), message);
    }

    @Override
    public String lock(String key, String value, String minutes) {
        if (StringUtils.isNotBlank(minutes)) {
            return String.valueOf(redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofMinutes(Integer.parseInt(minutes))));
        } else {
            return String.valueOf(redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofMinutes(5)));
        }
    }
}
