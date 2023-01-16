package com.elpsykongroo.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@MockServerTest("server.url=http://localhost:${mockServerPort}")
@ExtendWith(SpringExtension.class)
public class RedisServiceTest {
    private MockServerClient client;

    @Value("${server.url}")
    private String serverUrl;

    @Test
    void redisTest() {
        client.when(request().withMethod("POST").withPath("/redis/set"))
                .respond(response().withStatusCode(200));
        client.when(request().withMethod("POST").withPath("/redis/get"))
                .respond(response().withStatusCode(200));
        WebTestClient webTestClient = WebTestClient.bindToServer().baseUrl(serverUrl).build();
        webTestClient.post().uri("/redis/set").exchange().expectStatus().isOk();
        webTestClient.post().uri("/redis/get").exchange().expectStatus().isOk();

    }
//   @Test
//   void set() {
//       this.server.expect(requestTo("http://localhost:8379/redis/set")).andRespond(withSuccess());
//       this.redisService.set(new KV());
//   }
//   @Test
//   void get() {
//       this.server.expect(requestTo("http://localhost:8379/redis/get")).andRespond(withSuccess());
//       this.redisService.get(new KV());
//   }
}
