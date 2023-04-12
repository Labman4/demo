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

package com.elpsykongroo.auth.server.entity.authorization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "`authorization`")
public class Authorization {
	@Id
	private String id;

	private String registeredClientId;

	private String principalName;

	private String authorizationGrantType;

	@Column(length = 1000)
	private String authorizedScopes;

	@Column(length = 4000)
	private String attributes;

	@Column(length = 500)
	private String state;

	@Column(length = 1000)
	private String authorizationCodeValue;

	private Instant authorizationCodeIssuedAt;

	private Instant authorizationCodeExpiresAt;

	private String authorizationCodeMetadata;
	@Column(length = 2000)
	private String accessTokenValue;

	private Instant accessTokenIssuedAt;

	private Instant accessTokenExpiresAt;
	@Column(length = 1000)
	private String accessTokenMetadata;

	private String accessTokenType;

	@Column(length = 1000)
	private String accessTokenScopes;

	@Column(length = 1000)
	private String refreshTokenValue;

	private Instant refreshTokenIssuedAt;

	private Instant refreshTokenExpiresAt;

	@Column(length = 1000)
	private String refreshTokenMetadata;

	@Column(length = 2000)
	private String oidcIdTokenValue;

	private Instant oidcIdTokenIssuedAt;

	private Instant oidcIdTokenExpiresAt;

	@Column(length = 1000)
	private String oidcIdTokenMetadata;

	@Column(length = 2000)
	private String oidcIdTokenClaims;
}
