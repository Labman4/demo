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

package com.elpsykongroo.services.elasticsearch.server.config;

import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.services.elasticsearch.server.utils.SSLUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.vault.annotation.VaultPropertySource;

import javax.net.ssl.SSLContext;
import java.time.Duration;

@ConditionalOnProperty(
        prefix = "service",
        name = "vault",
        havingValue = "true",
        matchIfMissing = false)
@VaultPropertySource(value = "${SECRETS_PATH:database/creds/elastic-internal}")
@Configuration(proxyBeanMethods = false)
@EnableElasticsearchRepositories("com.elpsykongroo.services.elasticsearch.server.repo")
public class ElasticConfig extends ElasticsearchConfiguration {
    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    Environment env;
    
   @Override
   public ClientConfiguration clientConfiguration() {
       String username = env.getProperty("username");
       String password = env.getProperty("password");
       ServiceConfig.ES es =  serviceConfig.getElastic();
       if ("public".equals(es.getSsl().getType())) {
           return ClientConfiguration.builder()
                    .connectedTo(es.getNodes())
                    .usingSsl()
                    .withBasicAuth(username, password)
                    .withConnectTimeout(Duration.ofSeconds(es.getTimeout().getConnect()))
                    .withSocketTimeout(Duration.ofSeconds(es.getTimeout().getSocket()))
                    .build();
       } else if ("self".equals(es.getSsl().getType())) {
           SSLContext sslContext = SSLUtils.getSSLContext(es.getSsl().getCa(),
                   es.getSsl().getCert(), es.getSsl().getKey());
           return ClientConfiguration.builder()
                    .connectedTo(es.getNodes())
                    .usingSsl(sslContext)
                    .withBasicAuth(username, password)
                    .withConnectTimeout(Duration.ofSeconds(es.getTimeout().getConnect()))
                    .withSocketTimeout(Duration.ofSeconds(es.getTimeout().getSocket()))
                    .build();
       } else if (StringUtils.isNotEmpty(es.getUser())){
           return ClientConfiguration.builder()
                    .connectedTo(es.getNodes())
                    .withBasicAuth(username, password)
                    .withConnectTimeout(Duration.ofSeconds(es.getTimeout().getConnect()))
                    .withSocketTimeout(Duration.ofSeconds(es.getTimeout().getSocket()))
                    .build();
       } else {
           return ClientConfiguration.builder()
                    .connectedTo(es.getNodes())
                    .withConnectTimeout(Duration.ofSeconds(es.getTimeout().getConnect()))
                    .withSocketTimeout(Duration.ofSeconds(es.getTimeout().getSocket()))
                    .build();
      }
   }
}
