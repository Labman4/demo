package com.elpsykongroo.demo;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.elpsykongroo.demo.utils.JsonUtils;
import com.elpsykongroo.services.redis.RedisService;
import com.elpsykongroo.services.redis.dto.KV;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpForward;
import org.mockserver.springtest.MockServerTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.elpsykongroo.demo.config.ServiceConfig;
import com.elpsykongroo.demo.repo.elasticsearch.AccessRecordRepo;
import com.elpsykongroo.demo.repo.elasticsearch.IPRepo;

import javax.xml.crypto.dsig.keyinfo.KeyValue;

import static org.mockserver.model.HttpForward.forward;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.model.NottableString.not;


@Testcontainers
@MockServerTest("server.url=http://localhost:${mockServerPort}")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
public class WebClientTest  {

    @LocalServerPort
    int serverPort;

    MockServerClient client;

     @Value("${server.url}")
    private String serverUrl;

    @Autowired 
    WebTestClient webClient;

    @Container
    static ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.3")
                                .withExposedPorts(9200)
                                .withPassword("123456").withStartupTimeout(Duration.ofSeconds(100));

    @Test
    @Timeout(value = 200, unit = TimeUnit.SECONDS)
    void web() {
        client.when(request().withMethod("POST").withPath("/redis.*"))
                .respond(response().withStatusCode(200));
//        client.when(request().withPath(not("/redis.*")))
//                .forward(forward().withPort(serverPort));
//        WebTestClient webTestClient = WebTestClient.bindToServer().baseUrl(serverUrl).build();
        webClient
            .get()
            .uri("/public/ip")
                .exchange()
            .expectStatus().isOk();

        webClient
            .put()
            .uri("/ip/manage/add?address=test.elpsykongroo.com&black=false")
            .exchange()
            .expectAll(
                res -> res.expectStatus().isOk()
                // res -> res.expectBody().jsonPath("$.data").isNotEmpty()
            );
      
        webClient
            .get()
            .uri("/ip/manage/list?black=false&pageNumber=0&pageSize=10")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.data").isNotEmpty(); 

        webClient
            .get()
            .uri("/record/access?pageNumber=0&pageSize=10&order=0")
            .exchange()
            .expectStatus().isOk();

        webClient
            .delete()
            .uri("/record/delete?sourceIP=test.elpsykongroo.com&id=1")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.data").isNotEmpty();

        webClient
            .post()
            .uri("/record/filter?param=man&pageNumber=0&pageSize=10")
            .exchange()
            .expectStatus().isOk();

        webClient
            .patch()
            .uri("/ip/manage/patch?address=test.elpsykongroo.com&black=false&id=1")
            .exchange()
            .expectStatus().isOk();
    }
    
    @BeforeAll
    static void setUp() throws Exception {
         elastic.start();
        //  System.setProperty("spring.data.redis.cluster.nodes", redis.getHost() + ":" + redis.getMappedPort(6379).toString());
        //  System.setProperty("spring.data.redis.port", redis.getMappedPort(6379).toString());
      //    System.setProperty("spring.data.redis.password", "123456");
    }
    
    @DynamicPropertySource
    static void overrideTestProperties(DynamicPropertyRegistry registry) {
        registry.add("service.es.nodes", elastic::getHttpHostAddress);
        // registry.add("service..es.ssl.type", serviceConfig.getEs().getSsl()::getType);

    }
    @AfterAll
    static void destroy() {
         elastic.stop();
    }
}
