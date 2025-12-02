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

package com.elpsykongroo.services.elasticsearch.config;

import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.services.elasticsearch.utils.SSLUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.ssl.SSLContexts;
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
@VaultPropertySource(value = "${SECRETS_DATA_ES_PATH:database/creds/elastic}", renewal = VaultPropertySource.Renewal.ROTATE)
@Configuration(proxyBeanMethods = false)
@EnableElasticsearchRepositories
public class ElasticConfig extends ElasticsearchConfiguration {

    @Autowired
    Environment env;
    @Autowired
    private ServiceConfig serviceConfig;

   @Override
   public ClientConfiguration clientConfiguration() {
       String username = env.getProperty("username");
       String password = env.getProperty("password");
       String type = serviceConfig.getEs().getSsl().getType();
       String ca = serviceConfig.getEs().getSsl().getCa();
       String key = serviceConfig.getEs().getSsl().getKey();
       String cert = serviceConfig.getEs().getSsl().getCert();
       String[] nodes = serviceConfig.getEs().getNodes();
       long connect = serviceConfig.getEs().getTimeout().getConnect();
       long socket = serviceConfig.getEs().getTimeout().getSocket();
       String user = serviceConfig.getEs().getUser();
       String pass = serviceConfig.getEs().getPass();
       if (StringUtils.isNotBlank(pass)) {
           password = pass;
       }
       if (StringUtils.isNotBlank(user)) {
           username = user;
       }
       if (StringUtils.isBlank(password)) {
           return ClientConfiguration.builder()
                   .connectedTo(nodes)
                   .withConnectTimeout(Duration.ofSeconds(connect))
                   .withSocketTimeout(Duration.ofSeconds(socket))
                   .build();
       }
       if ("public".equals(type)) {
           return ClientConfiguration.builder()
                    .connectedTo(nodes)
                    .usingSsl()
                    .withBasicAuth(username, password)
                    .withConnectTimeout(Duration.ofSeconds(connect))
                    .withSocketTimeout(Duration.ofSeconds(socket))
                    .build();
       } else if ("self".equals(type)) {
           SSLContext sslContext = SSLUtils.getSSLContext(ca,
                   cert, key);
           return ClientConfiguration.builder()
                    .connectedTo(nodes)
                    .usingSsl(sslContext)
                    .withBasicAuth(username, password)
                    .withConnectTimeout(Duration.ofSeconds(connect))
                    .withSocketTimeout(Duration.ofSeconds(socket))
                    .build();
       } else if ("noVerify".equals(type)) {
           SSLContext sslContext = SSLContexts.createDefault();
           return ClientConfiguration.builder()
                   .connectedTo(nodes)
                   .usingSsl(sslContext, (hostname, session) -> true)
                   .withBasicAuth(username, password)
                   .withConnectTimeout(Duration.ofSeconds(connect))
                   .withSocketTimeout(Duration.ofSeconds(socket))
                   .build();
       } else if (StringUtils.isNotEmpty(username)){
           return ClientConfiguration.builder()
                    .connectedTo(nodes)
                    .withBasicAuth(username, password)
                    .withConnectTimeout(Duration.ofSeconds(connect))
                    .withSocketTimeout(Duration.ofSeconds(socket))
                    .build();
       } else {
           return ClientConfiguration.builder()
                    .connectedTo(nodes)
                    .withConnectTimeout(Duration.ofSeconds(connect))
                    .withSocketTimeout(Duration.ofSeconds(socket))
                    .build();
      }
   }
}
