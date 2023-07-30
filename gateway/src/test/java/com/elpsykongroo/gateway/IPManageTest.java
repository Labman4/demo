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
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

class IPManageTest extends BaseTest{
    @BeforeEach
    @Override
    public void setup() {
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(MockMvcRequestBuilders.get("/").with(SecurityMockMvcRequestPostProcessors.csrf()))
                .configureClient()
                .build();
        client.when(HttpRequest.request().withPath("/redis/key.*").withMethod("GET"))
                .respond(HttpResponse.response().withStatusCode(200));
        client.when(HttpRequest.request().withPath("/redis/key.*").withMethod("PUT"))
                .respond(HttpResponse.response().withStatusCode(200));
        client.when(HttpRequest.request().withPath("/search"))
                .respond(HttpResponse.response().withStatusCode(200));
    }

    @Test
    void ipList() {
        webTestClient
                .get()
                .uri("/ip?black=false&pageNumber=0&pageSize=10&order=0")
                .exchange()
                .expectStatus().isOk();
//            .expectBody().jsonPath("$.data").isNotEmpty();
        webTestClient
                .get()
                .uri("/ip?black=true&pageNumber=0&pageSize=10&order=0")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .get()
                .uri("/ip?black=&pageNumber=0&pageSize=10&order=0")
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
        client.when(HttpRequest.request().withPath("/search"))
                .respond(HttpResponse.response().withStatusCode(200).withBody("1"));
        client.when(HttpRequest.request().withPath("/redis/key.*").withMethod("GET"))
                .respond(HttpResponse.response().withStatusCode(200).withBody("ip.elpsykongroo.com"));
        webTestClient
                .patch()
                .uri("/ip?address=localhost&black=false&id=")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .patch()
                .uri("/ip?address=localhost&black=&id=")
                .exchange()
                .expectStatus().isOk();
    }
}
