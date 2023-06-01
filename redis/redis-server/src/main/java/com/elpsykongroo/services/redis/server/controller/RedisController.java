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


package com.elpsykongroo.services.redis.server.controller;

import com.elpsykongroo.services.redis.server.entity.KV;
import com.elpsykongroo.services.redis.server.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("redis")
public class RedisController {
    @Autowired
    private RedisService redisService;

    @PostMapping("set")
    public void set(@RequestBody KV kv) {
        redisService.setCache(kv.getKey(), kv.getValue(), kv.getTime());
    }

    @GetMapping("get")
    public String get(@RequestParam("key") String key) {
        return redisService.getCache(key);
    }

    @GetMapping("get/token")
    public String getToken(@RequestParam("key") String key) {
        return redisService.getToken(key);
    }
}
