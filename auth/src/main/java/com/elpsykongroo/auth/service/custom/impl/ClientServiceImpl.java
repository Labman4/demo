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

package com.elpsykongroo.auth.service.custom.impl;

import com.elpsykongroo.auth.entity.client.Client;
import com.elpsykongroo.auth.repository.client.ClientRepository;
import com.elpsykongroo.auth.service.custom.ClientService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ClientServiceImpl implements ClientService {
    @Autowired
    private ClientRepository clientRepository;

    @Override
    public String add(Client client) {
        Optional<Client> registryClient = clientRepository.findByClientId(client.getClientId());
           if (!registryClient.isPresent()) {
               client.setClientIdIssuedAt(Instant.now());
               return clientRepository.save(client).getClientId();
           } else {
               return String.valueOf(updateClient(client));
           }
    }

    @Override
    @Transactional
    public String delete(String clientId) {
        return clientRepository.deleteByClientId(clientId);
    }

    @Override
    public int update(Client client) {
        return updateClient(client);
    }

    private int updateClient(Client client) {
        return clientRepository.updateByClientId(
                client.getClientSecret(),
                client.getClientSecretExpiresAt(),
                client.getClientName(),
                client.getClientAuthenticationMethods(),
                client.getAuthorizationGrantTypes(),
                client.getRedirectUris(),
                client.getPostLogoutRedirectUris(),
                client.getScopes(),
                client.getClientSettings(),
                client.getTokenSettings(),
                client.getClientId());
    }

    @Override
    public List<Client> findAll() {
        return clientRepository.findAll();
    }
}
