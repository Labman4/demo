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

import com.elpsykongroo.auth.entity.client.ClientRegistry;
import com.elpsykongroo.auth.repository.client.ClientRegistryRepository;
import com.elpsykongroo.auth.service.custom.ClientRegistrationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientRegistrationServiceImpl implements ClientRegistrationService {
    @Autowired
    private ClientRegistryRepository clientRegistryRepository;

    @Override
    public String add(ClientRegistry client) {
       ClientRegistry clientRegistry = clientRegistryRepository.findByRegistrationId(client.getRegistrationId());
       if (clientRegistry == null) {
          return clientRegistryRepository.save(client).getRegistrationId();
       } else {
           return String.valueOf(update(client));
       }
    }

    @Override
    public int update(ClientRegistry clientRegistry) {
        return updateClientRegistry(clientRegistry);
    }

    private int updateClientRegistry(ClientRegistry clientRegistry) {
        return clientRegistryRepository.updateByRegistrationId(
                clientRegistry.getClientId(),
                clientRegistry.getClientSecret(),
                clientRegistry.getClientAuthenticationMethod(),
                clientRegistry.getAuthorizationGrantType(),
                clientRegistry.getRedirectUri(),
                clientRegistry.getScopes(),
                clientRegistry.getProviderDetails(),
                clientRegistry.getClientName(),
                clientRegistry.getRegistrationId());
    }
    @Override
    @Transactional
    public String delete(String registrationId) {
        return clientRegistryRepository.deleteByRegistrationId(registrationId);
    }

    @Override
    public List<ClientRegistry> findAll() {
        return clientRegistryRepository.findAll();
    }


}
