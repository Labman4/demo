/*
 * Copyright 2022 the original author or authors.
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;


import com.elpsykongroo.auth.entity.client.Client;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

public interface ClientRepository extends CrudRepository<Client, String> {
	@Transactional
	@Modifying
	@Query("""
			update Client c set c.clientSecret = ?1, c.clientSecretExpiresAt = ?2, c.clientName = ?3, c.clientAuthenticationMethods = ?4, c.authorizationGrantTypes = ?5, c.redirectUris = ?6, c.postLogoutRedirectUris = ?7, c.scopes = ?8, c.clientSettings = ?9, c.tokenSettings = ?10
			where c.clientId = ?11""")
	int updateByClientId(String clientSecret, Instant clientSecretExpiresAt, String clientName, String clientAuthenticationMethods, String authorizationGrantTypes, String redirectUris, String postLogoutRedirect, String scopes, String clientSettings, String tokenSettings, @NonNull String clientId);

	Optional<Client> findByClientId(String clientId);

	String deleteByClientId(String clientId);

	@Override
	List<Client> findAll();
}
