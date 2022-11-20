
//package com.elpsykongroo.demo.config;
//
//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import org.elasticsearch.client.RestHighLevelClient;
//
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//import org.springframework.data.elasticsearch.client.ClientConfiguration;
//import org.springframework.data.elasticsearch.client.RestClients;
//import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
//import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
//import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
//import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
//import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
//import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter;
//import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
//
///**
// * <p>ElasticConfig class.</p>
// *
// * @author labman1
// * @version $Id: $Id
// */
//@Configuration
//public class ElasticConfig extends ElasticsearchConfiguration {
//    @Value("${ELASTICSEARCH_URL}")
//    private String ES_URL;
//
//    @Value("${ELASTICSEARCH_USER}")
//    private String ES_USER;
//
//    @Value("${ELASTICSEARCH_PASS}")
//    private String ES_PASS;
//
////    private SSLConfig sslConfig;
////
////    public ElasticConfig() throws Exception {
////        sslConfig = new SSLConfig();
////    }
//    @Override
//    public ClientConfiguration clientConfiguration() {
////        SSLContext sslContext = null;
////        try {
////            sslContext = sslConfig.getSSLContext();
////        } catch (Exception e) {
////            e.printStackTrace();
////        }
//        return ClientConfiguration.builder()
//                .connectedTo(ES_URL)
//                .withBasicAuth(ES_USER, ES_PASS)
//                .build();
//    }
//    @Override
//    @Bean
//    public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
//            ElasticsearchClient elasticsearchClient) {
//
//        ElasticsearchTemplate template = new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter);
//        template.setRefreshPolicy(refreshPolicy());
//
//        return template;
//    }
//
////    @Bean
////    public ElasticsearchTemplate elasticsearchTemplate(ElasticsearchConverter elasticsearchConverter,
////            ElasticsearchClient elasticsearchClient) {
////
////        ElasticsearchTemplate template = new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter);
////        template.setRefreshPolicy(refreshPolicy());
////
////        return template;
////    }
//
////    @Override
////    @Bean
////    public RestHighLevelClient elasticsearchClient () {
////       ClientConfiguration clientConfiguration =  ClientConfiguration.builder()
////                .connectedTo(ES_URL)
////                .withBasicAuth(ES_USER, ES_PASS)
////                .build();
////        return RestClients.create(clientConfiguration).rest();
////    }
////
////    @Bean(name = {"elasticsearchOperations,elasticsearchTemplate" })
////    public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
////            RestHighLevelClient elasticsearchClient) {
////
////        ElasticsearchRestTemplate template = new ElasticsearchRestTemplate(elasticsearchClient, elasticsearchConverter);
////        template.setRefreshPolicy(refreshPolicy());
////
////        return template;
////    }
//}
