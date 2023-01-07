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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.vault.annotation.VaultPropertySource;



@Configuration(proxyBeanMethods = false)
@VaultPropertySource("${SECRETS_PATH:}/${ENV:dev}")
@Profile({ "dev", "prod" })
// @Slf4j
public class SecretsConfig {

    @Autowired
    Environment env;
    
    // @Bean
    // public void test() {
    //     Secrets secrets = new Secrets();
    //     secrets.setEsPass(env.getProperty("service.es.pass"));
    //     secrets.setEsUser(env.getProperty("service.es.user"));
    //     secrets.setRedisPass(env.getProperty("spring.data.redis.password"));
    //     log.info("vault secret:{}", secrets.toString());    
    // }
}
