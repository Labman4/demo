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

import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.model.MediaType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class AccessRecordTest extends BaseTest {
    @BeforeEach
    void setup() {
        super.setup();
        AccessRecord accessRecord = new AccessRecord();
        accessRecord.setSourceIP("1.1.1.1");
        accessRecord.setAccessPath("/path");
        accessRecord.setUserAgent("postman");
        accessRecord.setId("1");
        Map<String, String> header = new HashMap<>();
        header.put("x-real-ip", "127.0.0.1");
        accessRecord.setRequestHeader(header);
        String records = JsonUtils.toJson(Collections.singleton(accessRecord));
        client.when(request().withPath("/search/record/list.*"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(records, MediaType.APPLICATION_JSON));
        client.when(request().withPath("/search/record.*"))
                .respond(response().withStatusCode(200));
    }
    @Test
//    @Timeout(value = 200, unit = TimeUnit.SECONDS)
    void list() {
        webTestClient
            .get()
            .uri("/record/access?pageNumber=0&pageSize=10&order=0")
            .exchange()
            .expectStatus().isOk();
//            .expectBody().jsonPath("$.data").isNotEmpty();
    }


    @Test
    void delete() {
        webTestClient
            .delete()
            .uri("/record/delete?sourceIP=ip.elpsykongroo.com&id=1")
            .exchange()
            .expectStatus().isOk();
        webTestClient
                .delete()
                .uri("/record/delete?sourceIP=test.elpsykongroo.com&id=1")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void filter() {
        webTestClient
            .post()
            .uri("/record/filter?param=man&pageNumber=0&pageSize=10")
            .exchange()
            .expectStatus().isOk();
        webTestClient
                .post()
                .uri("/record/filter?param=test.elpsykongroo.com&pageNumber=0&pageSize=10")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .post()
                .uri("/record/filter?param=127.0.0.1&pageNumber=0&pageSize=10")
                .exchange()
                .expectStatus().isOk();
        webTestClient
                .post()
                .uri("/record/filter?param=ip.elpsykongroo.com&pageNumber=0&pageSize=10")
                .exchange()
                .expectStatus().isOk();
    }
}
