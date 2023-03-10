package com.elpsykongroo.auth.server.service.client;

import com.elpsykongroo.auth.server.entity.client.ClientRegistry;
import com.elpsykongroo.auth.server.repository.client.ClientRegistryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.stream.Collectors;

@Service
public class JpaClientRegistrationRepository implements ClientRegistrationRepository,Iterable<ClientRegistration>  {
    @Autowired
    private ClientRegistryRepository clientRegistryRepository;

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        ClientRegistry client = clientRegistryRepository.findByRegistrationId(registrationId);
        ClientRegistration clientRegistration = convertToClientRegistration(client);
        return clientRegistration;
    }

    private static ClientRegistration convertToClientRegistration(ClientRegistry client) {
        ClientRegistration clientRegistration = ClientRegistration
                .withRegistrationId(client.getRegistrationId())
                .clientId(client.getClientId())
                .clientSecret(client.getClientSecret())
                .clientName(client.getClientName())
                .authorizationGrantType(client.getAuthorizationGrantType())
                .clientAuthenticationMethod(client.getClientAuthenticationMethod())
                .scope(client.getScopes())
                .redirectUri(client.getRedirectUri())
                .jwkSetUri(client.getProviderDetails().getJwkSetUri())
                .userInfoUri(client.getProviderDetails().getUserInfoEndpoint().getUri())
                .userInfoAuthenticationMethod(client.getProviderDetails().getUserInfoEndpoint().getAuthenticationMethod())
                .userNameAttributeName(client.getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName())
                .issuerUri(client.getProviderDetails().getIssuerUri())
                .authorizationUri(client.getProviderDetails().getAuthorizationUri())
                .tokenUri(client.getProviderDetails().getTokenUri())
                .providerConfigurationMetadata(client.getProviderDetails().getConfigurationMetadata())
                .build();
        return clientRegistration;
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return clientRegistryRepository.findAll()
                .stream().map(clientRegistry -> convertToClientRegistration(clientRegistry))
                .collect(Collectors.toList()).iterator();
    }
}
