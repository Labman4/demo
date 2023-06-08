/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elpsykongroo.services.elasticsearch;

import com.elpsykongroo.services.elasticsearch.domain.AccessRecord;
import com.elpsykongroo.services.elasticsearch.domain.IPManage;
import com.elpsykongroo.services.elasticsearch.repo.AccessRecordRepo;
import com.elpsykongroo.services.elasticsearch.repo.IPRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.testcontainers.elasticsearch.ElasticsearchContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashMap;
import java.util.Map;

//@Testcontainers
@DataElasticsearchTest
@ActiveProfiles("test")
public class ElasticsearchTest {

//    @Container
//    static ElasticsearchContainer elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.17.3")
//            .withExposedPorts(9200)
//            .withPassword("elpsy").withStartupTimeout(Duration.ofSeconds(100));

//    @BeforeAll
//    static void setUp() throws Exception {
//        elastic.start();
//    }
//
//    @DynamicPropertySource
//    static void overrideTestProperties(DynamicPropertyRegistry registry) {
//        registry.add("service.es.nodes", elastic::getHttpHostAddress);
//    }
//    @AfterAll
//    static void destroy() {
//        elastic.stop();
//    }

    @Autowired
    private IPRepo ipRepo;

    @Autowired
    private AccessRecordRepo accessRecordRepo;


    @Test
    void elastic() {
        IPManage ipManage = new IPManage();
        ipManage.setBlack(false);
        ipManage.setAddress("127.0.0.1");
        ipRepo.save(ipManage);
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

        AccessRecord accessRecord = new AccessRecord();
        accessRecord.setSourceIP("1.1.1.1");
        accessRecord.setAccessPath("/path");
        accessRecord.setUserAgent("postman");
        Map<String, String> header = new HashMap<>();
        header.put("x-real-ip", "127.0.0.1");
        accessRecord.setRequestHeader(header);
        accessRecordRepo.save(accessRecord);
        accessRecordRepo.findBySourceIP("127.0.0.1");
        accessRecordRepo.searchSimilar(accessRecord, new String[]{accessRecord.getAccessPath()}, Pageable.ofSize(1));
        accessRecordRepo.deleteAll();
    }
}