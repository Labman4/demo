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

package com.elpsykongroo.auth.repository.client;

import com.elpsykongroo.auth.entity.client.ClientRegistry;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
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

    @Override
    List<ClientRegistry> findAll();
}
