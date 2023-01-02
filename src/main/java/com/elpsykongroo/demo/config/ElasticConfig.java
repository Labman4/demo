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

package com.elpsykongroo.demo.config;

import com.elpsykongroo.demo.utils.SSLUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import javax.net.ssl.SSLContext;
import java.time.Duration;

@Configuration
@EnableElasticsearchRepositories("com.elpsykongroo.demo.repo")
public class ElasticConfig extends ElasticsearchConfiguration {
   @Autowired
   private ServiceConfig serviceConfig;
   
   @Override
   public ClientConfiguration clientConfiguration() {
       ServiceConfig.ES es= serviceConfig.getEs();
       if ("public".equals(es.getSsl().getType())) {
           return ClientConfiguration.builder()
                   .connectedTo(serviceConfig.getEs().getNodes())
                   .usingSsl()
                   .withBasicAuth(es.getUser(), es.getPass())
                   .withConnectTimeout(Duration.ofSeconds(Integer.parseInt(es.getTimeout().getConnect())))
                   .withSocketTimeout(Duration.ofSeconds(Integer.parseInt(es.getTimeout().getSocket())))
                   .build();
       } else if ("self".equals(es.getSsl().getType())) {
           SSLContext sslContext = SSLUtils.getSSLContext(es.getSsl().getCa(),
                   es.getSsl().getCert(), es.getSsl().getKey());
           return ClientConfiguration.builder()
                   .connectedTo(serviceConfig.getEs().getNodes())
                   .usingSsl(sslContext)
                   .withBasicAuth(es.getUser(), es.getPass())
                   .withConnectTimeout(Duration.ofSeconds(Integer.parseInt(es.getTimeout().getConnect())))
                   .withSocketTimeout(Duration.ofSeconds(Integer.parseInt(es.getTimeout().getSocket())))
                   .build();
       } else {
           return ClientConfiguration.builder()
                   .connectedTo(serviceConfig.getEs().getNodes())
                   .withBasicAuth(es.getUser(), es.getPass())
                   .withConnectTimeout(Duration.ofSeconds(Integer.parseInt(es.getTimeout().getConnect())))
                   .withSocketTimeout(Duration.ofSeconds(Integer.parseInt(es.getTimeout().getSocket())))
                   .build();
       }


   }
}
