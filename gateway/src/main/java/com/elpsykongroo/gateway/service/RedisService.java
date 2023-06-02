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

package com.elpsykongroo.gateway.service;

import feign.Param;
import feign.RequestLine;
import org.springframework.web.bind.annotation.PathVariable;

public interface RedisService {

    @RequestLine("PUT /redis/key?key={key}&value={value}&duration={duration}")
    void set(@Param("key") String key , @Param("value")String value, @Param("duration") String duration);

    @RequestLine("GET /redis/key/{key}")
    String get(@Param("key")String key);

    @RequestLine("GET /redis/token/{key}")
    String getToken(@PathVariable("key") String key);
}