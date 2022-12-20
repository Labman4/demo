package com.elpsykongroo.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

@Configuration
public class ElasticConfig extends ElasticsearchConfiguration {
    @Value("${ELASTICSEARCH_URL}")
    private String ES_URL;

    @Value("${ELASTICSEARCH_USER}")
    private String ES_USER;

    @Value("${ELASTICSEARCH_PASS}")
    private String ES_PASS;

//    private SSLConfig sslConfig;
//
//    public ElasticConfig() throws Exception {
//        sslConfig = new SSLConfig();
//    }
    @Override
    public ClientConfiguration clientConfiguration() {
//        SSLContext sslContext = null;
//        try {
//            sslContext = sslConfig.getSSLContext();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return ClientConfiguration.builder()
                .connectedTo(ES_URL)
//                .usingSsl()
                .withBasicAuth(ES_USER, ES_PASS)
                .build();
    }
}
