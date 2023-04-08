package com.elpsykongroo.services.elasticsearch.server;

import com.elpsykongroo.services.elasticsearch.client.domain.AccessRecord;
import com.elpsykongroo.services.elasticsearch.client.domain.IPManage;
import com.elpsykongroo.services.elasticsearch.server.repo.AccessRecordRepo;
import com.elpsykongroo.services.elasticsearch.server.repo.IPRepo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Testcontainers
@DataElasticsearchTest
public class ElasticsearchTest {

    @Container
    static ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.3")
            .withExposedPorts(9200)
            .withPassword("elpsy").withStartupTimeout(Duration.ofSeconds(100));

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

    @Autowired
    private IPRepo ipRepo;

    @Autowired
    private AccessRecordRepo accessRecordRepo;


    @Test
    void ip() {
        IPManage ipManage = new IPManage("127.0.0.1", false);
        ipRepo.save(new IPManage("127.0.0.1", false));
        ipRepo.save(new IPManage("127.0.0.1", true));
        ipRepo.searchSimilar(ipManage,new String[]{ipManage.getAddress()}, Pageable.ofSize(1));
        ipRepo.countByAddressAndIsBlackFalse("127.0.0.1");
        ipRepo.countByAddressAndIsBlackTrue("127.0.0.1");
        ipRepo.findByIsBlackTrue();
        ipRepo.findByIsBlackFalse();
        ipRepo.deleteByAddressAndIsBlackFalse("127.0.0.1");
        ipRepo.deleteByAddressAndIsBlackTrue("127.0.0.1");
        ipRepo.deleteAll();
    }

    @Test
    void access() {
        AccessRecord accessRecord = new AccessRecord();
        accessRecord.setSourceIP("1.1.1.1");
        accessRecord.setAccessPath("/path");
        accessRecord.setUserAgent("postman");
        Map<String, String> header = new HashMap<>();
        header.put("x-real-ip", "127.0.0.1");
        accessRecord.setRequestHeader(header);
        accessRecordRepo.save(accessRecord);
        accessRecordRepo.findAll();
        accessRecordRepo.findByAccessPathLike("path");
        accessRecordRepo.findBySourceIP("127.0.0.1");
        accessRecordRepo.findByUserAgentLike("man");
        accessRecordRepo.findByRequestHeaderLike("real");
        accessRecordRepo.searchSimilar(accessRecord, new String[]{accessRecord.getAccessPath()}, Pageable.ofSize(1));
        accessRecordRepo.deleteAll();
    }
}
