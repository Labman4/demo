/*
 * Copyright 2020-2022 the original author or authors.
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

package com.elpsykongroo.auth.service.client;

import com.elpsykongroo.auth.repository.client.ClientRegistryRepository;
import com.elpsykongroo.auth.entity.client.ClientRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.stream.Collectors;

@Service
public class JpaClientRegistrationRepository implements ClientRegistrationRepository,Iterable<ClientRegistration>  {
    @Autowired
    private ClientRegistryRepository clientRegistryRepository;

    private static ClientRegistration convertToClientRegistration(ClientRegistry client) {
        return ClientRegistration
                .withRegistrationId(client.getRegistrationId())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientName(client.getClientName())
                .authorizationGrantType(new AuthorizationGrantType(client.getAuthorizationGrantType()))
                .clientAuthenticationMethod(new ClientAuthenticationMethod(client.getClientAuthenticationMethod()))
                .scope(client.getScopes())
                .redirectUri(client.getRedirectUri())
                .jwkSetUri(client.getProviderDetails().getJwkSetUri())
                .userInfoUri(client.getProviderDetails().getUserInfoEndpoint().getUri())
                .userInfoAuthenticationMethod(new AuthenticationMethod(client.getProviderDetails().getUserInfoEndpoint().getAuthenticationMethod()))
                .userNameAttributeName(client.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
                .issuerUri(client.getProviderDetails().getIssuerUri())
                .authorizationUri(client.getProviderDetails().getAuthorizationUri())
                .tokenUri(client.getProviderDetails().getTokenUri())
                .providerConfigurationMetadata(client.getProviderDetails().getConfigurationMetadata())
                .build();
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        ClientRegistry client = clientRegistryRepository.findByRegistrationId(registrationId);
        if (client != null ) {
            return convertToClientRegistration(client);
        }
        return null;
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return clientRegistryRepository.findAll()
                .stream().map(clientRegistry -> convertToClientRegistration(clientRegistry))
                .collect(Collectors.toList()).iterator();
    }
}
