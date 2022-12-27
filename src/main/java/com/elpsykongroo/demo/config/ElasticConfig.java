// /*
//  * Copyright 2022-2022 the original author or authors.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *      https://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package com.elpsykongroo.demo.config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.data.elasticsearch.client.ClientConfiguration;
// import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
// import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

// import java.time.Duration;
// import java.util.List;

// @Configuration
// @EnableElasticsearchRepositories("com.elpsykongroo.demo.repo")
// public class ElasticConfig extends ElasticsearchConfiguration {
//    @Value("#{'${spring.elasticsearch.uris}'.split('-')}")
//    private List<String> ES_URL;

//    @Value("${spring.elasticsearch.username}")
//    private String ES_USER;

//    @Value("${spring.elasticsearch.password}")
//    private String ES_PASS;

// //    private SSLConfig sslConfig;
// //
// //    public ElasticConfig() throws Exception {
// //        sslConfig = new SSLConfig();
// //    }
//    @Override
//    public ClientConfiguration clientConfiguration() {
// //        SSLContext sslContext = null;
// //        try {
// //               = sslConfig.getSSLContext();
// //        } catch (Exception e) {
// //            e.printStackTrace();
// //        }
//        return ClientConfiguration.builder()
//                .connectedTo(ES_URL.toString())
//                .usingSsl()
//                .withBasicAuth(ES_USER, ES_PASS)
//                .withConnectTimeout(Duration.ofSeconds(10))
//                .withSocketTimeout(Duration.ofSeconds(10))
//                .build();
//    }
// }
