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

import org.junit.jupiter.api.BeforeAll;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.context.WebApplicationContext;


@AutoConfigureObservability
@MockServerTest("server.url=http://localhost:${mockServerPort}")
@SpringBootTest(properties = {
                    "service.url.redis=${server.url}",
                    "service.url.es=${server.url}",
                    "service.url.storage=${server.url}",
                    "service.url.auth=${server.url}"
                },
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@ActiveProfiles("test")
public class BaseTest {

    @LocalServerPort
    int serverPort;

    MockServerClient client;

    @Value("${server.url}")
    protected String serverUrl;

    @Autowired
    protected WebApplicationContext context;

    protected WebTestClient webTestClient;


    @BeforeAll
    void setup() {
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .defaultRequest(MockMvcRequestBuilders.get("/").with(SecurityMockMvcRequestPostProcessors.csrf()))
                .configureClient()
                .build();
        client.when(HttpRequest.request().withPath("/redis/key.*").withMethod("GET"))
                .respond(HttpResponse.response().withStatusCode(200));
        client.when(HttpRequest.request().withPath("/redis/key.*").withMethod("PUT"))
                .respond(HttpResponse.response().withStatusCode(200));
        client.when(HttpRequest.request().withPath("/redis/lock.*").withMethod("PUT"))
                .respond(HttpResponse.response().withStatusCode(200).withBody("true"));
        client.when(HttpRequest.request().withPath("/search"))
                .respond(HttpResponse.response().withStatusCode(200));
    }
}
