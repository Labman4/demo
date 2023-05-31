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

package com.elpsykongroo.gateway.config;

import com.elpsykongroo.services.elasticsearch.client.SearchService;
import com.elpsykongroo.services.elasticsearch.client.impl.SearchServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SearchServiceConfig {
    @Value("${service.url.es}")
    private String esUrl;

    @Autowired
    private RestTemplateBuilder restTemplateBuilder;

    @Bean
    public SearchService searchService() {
//        List<HttpMessageConverter<?>> converters = new ArrayList<>();
//        converters.add(new IPManageHttpMessageConverter());
//        converters.add(new AccessRecordHttpMessageConverter());
//        RestTemplateBuilderConfigurer configurer = new RestTemplateBuilderConfigurer();
//        RestTemplateBuilder restTemplateBuilder =  configurer.configure(new RestTemplateBuilder())
//                    .setConnectTimeout(Duration.ofSeconds(30))
//                    .setReadTimeout(Duration.ofSeconds(20)).detectRequestFactory(true);
////                    .additionalMessageConverters(converters);

        return new SearchServiceImpl(esUrl,  restTemplateBuilder);
    }
}
