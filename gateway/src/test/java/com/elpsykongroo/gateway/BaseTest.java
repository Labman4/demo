package com.elpsykongroo.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.context.WebApplicationContext;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@MockServerTest("server.url=http://localhost:${mockServerPort}")
@SpringBootTest(properties = {
        "service.redis.url=${server.url}",
        "service.auth.url=${server.url}",
        "service.storage.url=${server.url}",
        "service.es.url=${server.url}"
},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class BaseTest {

    @LocalServerPort
    int serverPort;

    MockServerClient client;

    @Value("${server.url}")
    private String serverUrl;

    @Autowired
    private WebApplicationContext context;

    protected WebTestClient webTestClient;


    @BeforeEach
    void setup() {
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .defaultRequest(get("/").with(csrf()))
                .configureClient()
                .build();
        client.when(request().withMethod("POST").withPath("/redis.*"))
                .respond(response().withStatusCode(200));
        client.when(request().withPath("/search/ip.*"))
                .respond(response().withStatusCode(200));
    }
}
