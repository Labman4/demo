package com.elpsykongroo.demo.web;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.elpsykongroo.demo.config.ServiceConfig;
import com.elpsykongroo.demo.repo.redis.IPListRepo;
import com.elpsykongroo.demo.repo.elasticsearch.AccessRecordRepo;
import com.elpsykongroo.demo.repo.elasticsearch.IPRepo;
import com.elpsykongroo.demo.testconfig.RedisConfig;



@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {RedisConfig.class})
@ActiveProfiles("test")
public class WebClientTest  {   
    @Autowired
    IPListRepo ipListRepo;

     @Autowired
    IPRepo ipRepo;

    @Autowired
    AccessRecordRepo accessRecordRepo;

    @Autowired 
    WebTestClient webClient;

    @Autowired
    static ServiceConfig serviceConfig;

    // @Container
    // public static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.0.7-alpine"))
    // .withExposedPorts(6379);

    @Container
    static ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.3")
                                .withExposedPorts(9200)
                                .withPassword("123456").withStartupTimeout(Duration.ofSeconds(100));

    @Test
    @Timeout(value = 200, unit = TimeUnit.SECONDS)
    void web() {
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
            .uri("ip/manage/list?black&pageNumber=0&pageSize=10")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.data").isNotEmpty(); 

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
            .uri("/record/filter?param=man&pageNumber=0&pageSize=10")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.data").isNotEmpty();

        webClient
            .patch()
            .uri("ip/manage/patch?address=test.elpsykongroo.com&black=false&id")
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
        registry.add("service..es.nodes", elastic::getHttpHostAddress);
        // registry.add("service..es.ssl.type", serviceConfig.getEs().getSsl()::getType);

    }
    @AfterAll
    static void destroy() {
         elastic.stop();
    }
}
