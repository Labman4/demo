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
                .uri("/ip/manage/list?black=false&pageNumber=0&pageSize=10")
                .exchange()
                .expectStatus().isOk();
//            .expectBody().jsonPath("$.data").isNotEmpty();
    }

    @Test
    void ipAdd() {
        webTestClient
                .put()
                .uri("/ip/manage/add?address=ip.elpsykongroo.com&black=false")
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
                .uri("/ip/manage/patch?address=test.elpsykongroo.com&black=false&id=1")
                .exchange()
                .expectStatus().isOk();
    }
}
