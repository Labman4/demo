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

public class IPmanageTest extends BaseTest{
    @BeforeEach
    public void setup() {
        super.setup();
    }

    @Test
    void ipList() {
        webTestClient
                .get()
                .uri("/ip?black=false&pageNumber=0&pageSize=10")
                .exchange()
                .expectStatus().isOk();
//            .expectBody().jsonPath("$.data").isNotEmpty();
        webTestClient
                .get()
                .uri("/ip?black=true&pageNumber=0&pageSize=10")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .get()
                .uri("/ip?black=&pageNumber=0&pageSize=10")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void ipAdd() {
        webTestClient
                .put()
                .uri("/ip?address=ip.elpsykongroo.com&black=false")
                .exchange()
                .expectAll(
                        res -> res.expectStatus().isOk()
                        // res -> res.expectBody().jsonPath("$.data").isNotEmpty()
                );
    }

    @Test
    void accessIP() {
        webTestClient
                .get()
                .uri("/public/ip")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void patchIP() {
        webTestClient
                .patch()
                .uri("/ip?address=ip.elpsykongroo.com&black=false&id=1")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .patch()
                .uri("/ip?address=localhost&black=false&id=")
                .exchange()
                .expectStatus().isOk();
    }
}
