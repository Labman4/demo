package com.elpsykongroo.auth.server.service.custom.impl;

import com.elpsykongroo.auth.server.entity.client.Client;
import com.elpsykongroo.auth.server.repository.client.ClientRepository;
import com.elpsykongroo.auth.server.service.custom.ClientService;
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
        try {
            Optional<Client> regirtryClient = clientRepository.findByClientId(client.getClientId());
           if (!regirtryClient.isPresent()) {
               client.setClientIdIssuedAt(Instant.now());
               clientRepository.save(client);
           } else {
               updateClient(client);
           }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "done";
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
        return clientRepository.updateClientSecretAndClientSecretExpiresAtAndClientNameAndClientAuthenticationMethodsAndAuthorizationGrantTypesAndRedirectUrisAndScopesAndClientSettingsAndTokenSettingsByClientId(
                client.getClientSecret(),
                client.getClientSecretExpiresAt(),
                client.getClientName(),
                client.getClientAuthenticationMethods(),
                client.getAuthorizationGrantTypes(),
                client.getRedirectUris(),
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
