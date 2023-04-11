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

package com.elpsykongroo.gateway;

import com.elpsykongroo.services.redis.client.dto.KV;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RedisTest extends BaseTest {
    @BeforeEach
    public void setup() {
        super.setup();
    }

    @Test
    void redis() {
        KV kv = new KV("test", "1");
        webTestClient
                .post()
                .uri("/redis/set")
                .bodyValue(kv)
                .exchange().expectStatus().isOk();
        webTestClient
                .get()
                .uri("/redis/get?key=test")
                .exchange()
                .expectStatus().isOk();
    }
}
