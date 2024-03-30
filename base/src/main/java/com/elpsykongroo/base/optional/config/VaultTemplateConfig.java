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

package com.elpsykongroo.base.optional.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.annotation.VaultPropertySource;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.KubernetesAuthentication;
import org.springframework.vault.authentication.KubernetesAuthenticationOptions;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;

import java.net.URI;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
    prefix = "service",
    name = "vault",
    havingValue = "true",
    matchIfMissing = false)
@VaultPropertySource("${SECRETS_APP_BASE_PATH:kv/app/dev/base}")
@VaultPropertySource("${SECRETS_APP_PATH}")
public class VaultTemplateConfig extends AbstractVaultConfiguration  {

    @Bean
    public ClientAuthentication clientAuthentication() {
         if ("prod".equals(getEnvironment().getProperty("ENV"))) {
            KubernetesAuthenticationOptions options = KubernetesAuthenticationOptions.builder()
                 .role(getEnvironment().getProperty("VAULT_ROLE")).build();
             return  new KubernetesAuthentication(options, restOperations());
         } else {
             return new TokenAuthentication(getEnvironment().getProperty("VAULT_TOKEN"));
         }
    }
    @Bean
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint endpoint = new VaultEndpoint();
        URI uri = URI.create(getEnvironment().getProperty("VAULT_URI"));
        endpoint.setHost(uri.getHost());
        endpoint.setPort(uri.getPort());
        endpoint.setScheme(uri.getScheme());
        return endpoint;
    }
}
