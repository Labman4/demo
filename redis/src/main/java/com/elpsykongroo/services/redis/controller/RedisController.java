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


package com.elpsykongroo.services.redis.controller;

import com.elpsykongroo.services.redis.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("redis")
public class RedisController {
    @Autowired
    private RedisService redisService;

    @PutMapping("key")
    public void set(@RequestParam String key,
                    @RequestParam String value,
                    @RequestParam String duration) {
        redisService.setCache(key, value, duration);
    }

    @GetMapping("key/{key}")
    public String get(@PathVariable String key) {
        return redisService.getCache(key);
    }

    @GetMapping("token/{key}")
    public String getToken(@PathVariable String key) {
        return redisService.getToken(key);
    }

    @PutMapping("topic")
    public void publish (@RequestParam String topic,
                         @RequestParam String message,
                         @RequestParam String callback) {
        redisService.publish(topic, message, callback);
    }

    @PutMapping("lock")
    public String lock (@RequestParam String key,
                        @RequestParam String value,
                        @RequestParam String duration) {
        return redisService.lock(key, value, duration);
    }
}
