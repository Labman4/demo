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

 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
 import org.springframework.boot.web.client.RestTemplateBuilder;
 import org.springframework.context.annotation.Bean;
 import org.springframework.context.annotation.Configuration;

 import java.time.Duration;

 @Configuration(proxyBeanMethods = false)
 public class RestTemplateConfig {
      @Value("${service.timeout.connect:10}")
      private int connect;

      @Value("${service.timeout.read:10}")
      private int read;

      @Bean
      public RestTemplateBuilder restTemplateBuilder(RestTemplateBuilderConfigurer configurer) {
          return configurer.configure(new RestTemplateBuilder())
                  .setConnectTimeout(Duration.ofSeconds(connect))
                  .setReadTimeout(Duration.ofSeconds(read)).detectRequestFactory(true);
      }

 }
