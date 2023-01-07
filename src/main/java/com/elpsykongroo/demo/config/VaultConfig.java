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

import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.context.annotation.Profile;
import org.springframework.vault.config.AbstractVaultConfiguration;
import java.net.URI;

@Configuration(proxyBeanMethods = false)
@Profile({ "dev", "prod" })
public class VaultConfig extends AbstractVaultConfiguration  {
    @Override
    public ClientAuthentication clientAuthentication() {
        return new TokenAuthentication(getEnvironment().getProperty("vaultToken"));
    }
    @Override
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint endpoint = new VaultEndpoint();
        URI uri = URI.create(getEnvironment().getProperty("vaultUri"));
        endpoint.setHost(uri.getHost());
        endpoint.setPort(uri.getPort());
        endpoint.setScheme(uri.getScheme());
        endpoint.setPath(getEnvironment().getProperty("vaultPath"));
        return endpoint;
    }
}
