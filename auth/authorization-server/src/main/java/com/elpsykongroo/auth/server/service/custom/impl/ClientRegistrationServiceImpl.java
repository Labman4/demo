package com.elpsykongroo.auth.server.service.custom.impl;

import com.elpsykongroo.auth.server.entity.client.ClientRegistry;
import com.elpsykongroo.auth.server.repository.client.ClientRegistryRepository;
import com.elpsykongroo.auth.server.service.custom.ClientRegistrationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
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
                new ClientAuthenticationMethod(clientRegistry.getClientAuthenticationMethod()),
                new AuthorizationGrantType(clientRegistry.getAuthorizationGrantType()),
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
