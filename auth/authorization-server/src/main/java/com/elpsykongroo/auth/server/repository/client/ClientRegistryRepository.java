package com.elpsykongroo.auth.server.repository.client;

import com.elpsykongroo.auth.server.entity.client.ClientRegistry;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface ClientRegistryRepository extends CrudRepository<ClientRegistry, String> {
    @Transactional
    @Modifying
    @Query("""
            update ClientRegistry c set c.clientId = ?1, c.clientSecret = ?2, c.clientAuthenticationMethod = ?3, c.authorizationGrantType = ?4, c.redirectUri = ?5, c.scopes = ?6, c.providerDetails = ?7, c.clientName = ?8
            where c.registrationId = ?9""")
    int updateByRegistrationId(String clientId, String clientSecret,
                               String clientAuthenticationMethod, String authorizationGrantType,
                               String redirectUri, Set<String> scopes,
                               ClientRegistry.ProviderDetails providerDetails, String clientName,
                               @NonNull String registrationId);

    String deleteByRegistrationId(String registrationId);

    ClientRegistry findByRegistrationId(String registrationId);

    List<ClientRegistry> findAll();
}
