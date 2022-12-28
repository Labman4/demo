package com.elpsykongroo.demo.web;


import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.reactive.server.WebTestClient;


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class WebClientTest {
    @Test
    @Order(3)
    void testWithRedis(@Autowired WebTestClient webClient) {
        webClient
            .put()
            .uri("/ip/manage/add?address=test.elpsykongroo.com&black=false")
            .exchange()
            .expectAll(
                res -> res.expectStatus().isOk(),
                res -> res.expectBody().jsonPath("$.data").isNotEmpty()
            );
        webClient
            .patch()
            .uri("ip/manage/patch?address=test.elpsykongroo.com&black=false&id")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @Order(1)
    void ipTest(@Autowired WebTestClient webClient) {
        webClient
            .get()
            .uri("/public/ip")
            .exchange()
            .expectStatus().isOk();
        webClient
            .get()
            .uri("ip/manage/list?black&pageNumber=0&pageSize=10")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.data").isNotEmpty();
      
    }

    @Test
    @Order(2)
    void recordTest(@Autowired WebTestClient webClient) {
        webClient
            .get()
            .uri("/record/access?pageNumber=0&pageSize=10&order=0")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.data").isNotEmpty();
        webClient
            .delete()
            .uri("/record/delete?sourceIP=test.elpsykongroo.com&id")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.data").isNotEmpty();
        webClient
            .post()
            .uri("/record/filter?path=man")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.data").isEmpty();
    }
}
