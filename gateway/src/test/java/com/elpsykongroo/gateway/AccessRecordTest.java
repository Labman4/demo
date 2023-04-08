package com.elpsykongroo.gateway;

import com.elpsykongroo.gateway.utils.JsonUtils;
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
    }

    @Test
    void filter() {
        webTestClient
            .post()
            .uri("/record/filter?param=man&pageNumber=0&pageSize=10")
            .exchange()
            .expectStatus().isOk();
    }
}
