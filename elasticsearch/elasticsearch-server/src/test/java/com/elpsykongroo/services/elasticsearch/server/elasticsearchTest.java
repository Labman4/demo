package com.elpsykongroo.services.elasticsearch.server;

import com.elpsykongroo.services.elasticsearch.client.domain.AccessRecord;
import com.elpsykongroo.services.elasticsearch.client.domain.IPManage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class elasticsearchTest {
    @LocalServerPort
    int serverPort;

    @Autowired
    private TestRestTemplate restTemplate;

    private String recordPrefix =  "/search/record";

    private String ipPrefix =  "/search/ip";

    @BeforeAll
    static void setUp() throws Exception {
         elastic.start();
    }

    @DynamicPropertySource
    static void overrideTestProperties(DynamicPropertyRegistry registry) {
        registry.add("service.es.nodes", elastic::getHttpHostAddress);
    }
    @AfterAll
    static void destroy() {
         elastic.stop();
    }

    @Container
    static ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.3")
                                .withExposedPorts(9200)
                                .withPassword("elpsy").withStartupTimeout(Duration.ofSeconds(100));


    @Test
    public void findByIsBlackTrue() {
        ResponseEntity<List<IPManage>> response = restTemplate.exchange(
                ipPrefix + "/black/list",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IPManage>>() {});
    }

    @Test
    public void findByIsBlackFalse() {
        ResponseEntity<List<IPManage>> response = restTemplate.exchange(
                ipPrefix + "/white/list",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<IPManage>>() {});
    }

    @Test
    public void countByAddressAndIsBlackTrue() {
        HttpEntity<String> requestEntity = new HttpEntity("127.0.0.1");
        restTemplate.exchange(  ipPrefix + "/black/count",
                HttpMethod.GET,
                requestEntity,
                Long.class).getBody();
    }

    @Test
    public void countByAddressAndIsBlackFalse() {
        HttpEntity<String> requestEntity = new HttpEntity("127.0.0.1");
        restTemplate.exchange(ipPrefix + "/white/count",
                HttpMethod.GET,
                requestEntity,
                Long.class).getBody();
    }

    @Test
    public void deleteByAddressAndIsBlackTrue() {
        HttpEntity<String> requestEntity = new HttpEntity("127.0.0.1");
        restTemplate.exchange(ipPrefix + "/black/delete",
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();
    }

    @Test
    public void deleteByAddressAndIsBlackFalse() {
        HttpEntity<String> requestEntity = new HttpEntity("127.0.0.1");
        restTemplate.exchange( ipPrefix + "/white/delete",
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();
    }

    @Test
    public void findAllRecord() {
        Pageable pageable = PageRequest.of(0, 10);
        HttpEntity<Pageable> requestEntity = new HttpEntity(pageable);
        restTemplate.exchange(recordPrefix + "/list",
                HttpMethod.POST,
                requestEntity,
                String.class).getBody();
    }

    @Test
    public void findAllIP() {
        Pageable pageable = PageRequest.of(0, 10);
        HttpEntity<Pageable> requestEntity = new HttpEntity(pageable);
        restTemplate.exchange( ipPrefix + "/list",
                HttpMethod.POST,
                requestEntity,
                String.class).getBody();
    }

    @Test
    @Order(0)
    public void saveRecord() {
        AccessRecord accessRecord = new AccessRecord();
        accessRecord.setSourceIP("1.1.1.1");
        accessRecord.setAccessPath("/path");
        accessRecord.setUserAgent("postman");
        Map<String, String> header = new HashMap<>();
        header.put("x-real-ip", "127.0.0.1");
        accessRecord.setRequestHeader(header);
        HttpEntity<AccessRecord> requestEntity = new HttpEntity(accessRecord);
        restTemplate.exchange( recordPrefix + "/add",
                HttpMethod.PUT,
                requestEntity,
                String.class).getBody();
    }

    @Test
    @Order(1)
    public void saveIP() {
        IPManage ipManage = new IPManage("127.0.0.1", false);
        HttpEntity<IPManage> requestEntity = new HttpEntity(ipManage);
        String address = restTemplate.exchange( ipPrefix + "/add",
                HttpMethod.PUT,
                requestEntity,
                String.class).getBody();
    }

    @Test
    public void findByAccessPathLike() {
        ResponseEntity<List<AccessRecord>> response = restTemplate.exchange(
                recordPrefix + "/list/path?path=" + "ip",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<AccessRecord>>() {
                });
    }

    @Test
    public void findBySourceIP() {
        ResponseEntity<List<AccessRecord>> response = restTemplate.exchange(
                 recordPrefix + "/list/agent?agent=" + "127.0.0.1",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<AccessRecord>>() {
                });
    }

    @Test
    public void findByUserAgentLike() {
        ResponseEntity<List<AccessRecord>> response = restTemplate.exchange(
                 recordPrefix + "/list/agent?agent=" + "man",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<AccessRecord>>() {
                });
    }

    @Test
    public void findByRequestHeaderLike() {
        ResponseEntity<List<AccessRecord>> response = restTemplate.exchange(
                 recordPrefix + "/list/header?header=" + "x-real-ip",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<AccessRecord>>() {
                });
          }

    @Test
    public void deleteAllRecordById() {
        List<String> ids = new ArrayList<>();
        HttpEntity<List<String>> requestEntity = new HttpEntity(ids);
        restTemplate.exchange(recordPrefix + "/delete",
                HttpMethod.POST,
                requestEntity,
                String.class).getBody();

    }

    @Test
    public void deleteIPById() {
        HttpEntity<String> requestEntity = new HttpEntity("1");
        restTemplate.exchange( ipPrefix + "/delete",
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();
    }
}
