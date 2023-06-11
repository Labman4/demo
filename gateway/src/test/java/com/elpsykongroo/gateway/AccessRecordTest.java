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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccessRecordTest extends BaseTest {

    @BeforeEach
    @Override
    public void setup() {
        super.setup();
    }

    @Test
//    @Timeout(value = 200, unit = TimeUnit.SECONDS)
    void get() {
        webTestClient
            .get()
            .uri("/record?pageNumber=0&pageSize=10&order=0")
            .exchange()
            .expectStatus().isOk();
//            .expectBody().jsonPath("$.data").isNotEmpty();
    }


    @Test
    void delete() {
        webTestClient
            .delete()
            .uri("/record/ip.elpsykongroo.com")
            .exchange()
            .expectStatus().isOk();
        webTestClient
                .delete()
                .uri("/record/1")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .delete()
                .uri("/record/ ")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void post() {
        webTestClient
            .post()
            .uri("/record?params=man&pageNumber=0&pageSize=10&order=0")
            .exchange()
            .expectStatus().isOk();
        webTestClient
                .post()
                .uri("/record?params=test.elpsykongroo.com&pageNumber=0&pageSize=10&order=0")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .post()
                .uri("/record?params=127.0.0.1&pageNumber=0&pageSize=10&order=0")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .post()
                .uri("/record?params=ip.elpsykongroo.com&pageNumber=0&pageSize=10&order=0")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .post()
                .uri("/record?params=&pageNumber=0&pageSize=10&order=0")
                .exchange()
                .expectStatus().isOk();
    }
}
