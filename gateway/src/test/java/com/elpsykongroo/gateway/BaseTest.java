package com.elpsykongroo.gateway;

import org.junit.Before;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
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

@AutoConfigureObservability
@MockServerTest("server.url=http://localhost:${mockServerPort}")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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


    @Before
    void setup() {
        webTestClient = MockMvcWebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .defaultRequest(get("/").with(csrf()))
                .configureClient()
                .build();
        client.when(request().withPath("/redis/key.*").withMethod("GET"))
                .respond(response().withStatusCode(200));
        client.when(request().withPath("/redis/key.*").withMethod("PUT"))
                .respond(response().withStatusCode(200));
        client.when(request().withPath("/search/ip.*"))
                .respond(response().withStatusCode(200));
    }
}
