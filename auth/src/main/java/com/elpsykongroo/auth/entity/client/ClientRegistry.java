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

package com.elpsykongroo.auth.entity.client;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogMessage;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.core.AuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
@Entity
@Table( name = "`oauth2_client_registered`")
public class ClientRegistry {
    @Id
    private String registrationId;

    private String clientId;

    private String clientSecret;

    private String clientAuthenticationMethod;

    private String authorizationGrantType;

    private String redirectUri;

    private Set<String> scopes = Collections.emptySet();

    private ProviderDetails providerDetails = new ProviderDetails();

    private String clientName;

    public ClientRegistry() {
    }

    public static class ProviderDetails implements Serializable {

        private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

        private String authorizationUri;

        private String tokenUri;

        private UserInfoEndpoint userInfoEndpoint = new UserInfoEndpoint();

        private String jwkSetUri;

        private String issuerUri;

        private Map<String, Object> configurationMetadata = Collections.emptyMap();

        ProviderDetails() {
        }

        /**
         * Returns the uri for the authorization endpoint.
         *
         * @return the uri for the authorization endpoint
         */
        public String getAuthorizationUri() {
            return this.authorizationUri;
        }

        /**
         * Returns the uri for the token endpoint.
         *
         * @return the uri for the token endpoint
         */
        public String getTokenUri() {
            return this.tokenUri;
        }

        /**
         * Returns the details of the {@link UserInfoEndpoint UserInfo Endpoint}.
         *
         * @return the {@link UserInfoEndpoint}
         */
        public UserInfoEndpoint getUserInfoEndpoint() {
            return this.userInfoEndpoint;
        }

        /**
         * Returns the uri for the JSON Web Key (JWK) Set endpoint.
         *
         * @return the uri for the JSON Web Key (JWK) Set endpoint
         */
        public String getJwkSetUri() {
            return this.jwkSetUri;
        }

        /**
         * Returns the issuer identifier uri for the OpenID Connect 1.0 provider or the
         * OAuth 2.0 Authorization Server.
         *
         * @return the issuer identifier uri for the OpenID Connect 1.0 provider or the
         * OAuth 2.0 Authorization Server
         * @since 5.4
         */
        public String getIssuerUri() {
            return this.issuerUri;
        }

        /**
         * Returns a {@code Map} of the metadata describing the provider's configuration.
         *
         * @return a {@code Map} of the metadata describing the provider's configuration
         * @since 5.1
         */
        public Map<String, Object> getConfigurationMetadata() {
            return this.configurationMetadata;
        }

        /**
         * Details of the UserInfo Endpoint.
         */
        public static class UserInfoEndpoint implements Serializable {

            private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

            private String uri;

            private String authenticationMethod = AuthenticationMethod.HEADER.getValue();

            private String userNameAttributeName;

            UserInfoEndpoint() {
            }

            /**
             * Returns the uri for the user info endpoint.
             *
             * @return the uri for the user info endpoint
             */
            public String getUri() {
                return this.uri;
            }

            /**
             * Returns the authentication method for the user info endpoint.
             *
             * @return the {@link AuthenticationMethod} for the user info endpoint.
             * @since 5.1
             */
            public String getAuthenticationMethod() {
                return this.authenticationMethod;
            }

            /**
             * Returns the attribute name used to access the user's name from the user
             * info response.
             *
             * @return the attribute name used to access the user's name from the user
             * info response
             */
            public String getUserNameAttributeName() {
                return this.userNameAttributeName;
            }

        }

    }

    /**
     * A builder for {@link ClientRegistry}.
     */
    public final class Builder implements Serializable {

        private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

        private static final Log logger = LogFactory.getLog(Builder.class);

        private static final List<AuthorizationGrantType> AUTHORIZATION_GRANT_TYPES = Arrays.asList(
                AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.CLIENT_CREDENTIALS,
                AuthorizationGrantType.REFRESH_TOKEN);

        private String registrationId;

        private String clientId;

        private String clientSecret;

        private ClientAuthenticationMethod clientAuthenticationMethod;

        private AuthorizationGrantType authorizationGrantType;

        private String redirectUri;

        private Set<String> scopes;

        private String authorizationUri;

        private String tokenUri;

        private String userInfoUri;

        private AuthenticationMethod userInfoAuthenticationMethod = AuthenticationMethod.HEADER;

        private String userNameAttributeName;

        private String jwkSetUri;

        private String issuerUri;

        private Map<String, Object> configurationMetadata = Collections.emptyMap();

        private String clientName;

        private static AuthorizationGrantType resolveAuthorizationGrantType(String authorizationGrantType) {
            if (AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(authorizationGrantType)) {
                return AuthorizationGrantType.AUTHORIZATION_CODE;
            } else if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(authorizationGrantType)) {
                return AuthorizationGrantType.CLIENT_CREDENTIALS;
            } else if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(authorizationGrantType)) {
                return AuthorizationGrantType.REFRESH_TOKEN;
            }
            return new AuthorizationGrantType(authorizationGrantType);              // Custom authorization grant type
        }

        private static ClientAuthenticationMethod resolveClientAuthenticationMethod(String clientAuthenticationMethod) {
            if (ClientAuthenticationMethod.CLIENT_SECRET_BASIC.getValue().equals(clientAuthenticationMethod)) {
                return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
            } else if (ClientAuthenticationMethod.CLIENT_SECRET_POST.getValue().equals(clientAuthenticationMethod)) {
                return ClientAuthenticationMethod.CLIENT_SECRET_POST;
            } else if (ClientAuthenticationMethod.NONE.getValue().equals(clientAuthenticationMethod)) {
                return ClientAuthenticationMethod.NONE;
            }
            return new ClientAuthenticationMethod(clientAuthenticationMethod);      // Custom client authentication method
        }

        private static AuthenticationMethod resolveUserInfoAuthenticationMethod(String userInfoAuthenticationMethod) {
            if (AuthenticationMethod.FORM.getValue().equals(userInfoAuthenticationMethod)) {
                return AuthenticationMethod.FORM;
            } else if (AuthenticationMethod.HEADER.getValue().equals(userInfoAuthenticationMethod)) {
                return AuthenticationMethod.HEADER;
            } else {
                return AuthenticationMethod.QUERY.getValue().equals(userInfoAuthenticationMethod) ? AuthenticationMethod.QUERY : new AuthenticationMethod(userInfoAuthenticationMethod);
            }
        }
        private Builder(ClientRegistry clientRegistry) {
            this.registrationId = clientRegistry.registrationId;
            this.clientId = clientRegistry.clientId;
            this.clientSecret = clientRegistry.clientSecret;
            this.clientAuthenticationMethod = resolveClientAuthenticationMethod(clientRegistry.clientAuthenticationMethod);
            this.authorizationGrantType = resolveAuthorizationGrantType(clientRegistry.authorizationGrantType);
            this.redirectUri = clientRegistry.redirectUri;
            this.scopes = (clientRegistry.scopes != null) ? new HashSet<>(clientRegistry.scopes) : null;
            this.authorizationUri = clientRegistry.providerDetails.authorizationUri;
            this.tokenUri = clientRegistry.providerDetails.tokenUri;
            this.userInfoUri = clientRegistry.providerDetails.userInfoEndpoint.uri;
            this.userInfoAuthenticationMethod = resolveUserInfoAuthenticationMethod(clientRegistry.providerDetails.userInfoEndpoint.authenticationMethod);
            this.userNameAttributeName = clientRegistry.providerDetails.userInfoEndpoint.userNameAttributeName;
            this.jwkSetUri = clientRegistry.providerDetails.jwkSetUri;
            this.issuerUri = clientRegistry.providerDetails.issuerUri;
            Map<String, Object> configurationMetadata = clientRegistry.providerDetails.configurationMetadata;
            if (configurationMetadata != Collections.EMPTY_MAP) {
                this.configurationMetadata = new HashMap<>(configurationMetadata);
            }
            this.clientName = clientRegistry.clientName;
        }

        /**
         * Sets the client identifier.
         *
         * @param clientId the client identifier
         * @return the {@link Builder}
         */
        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        /**
         * Sets the scope(s) used for the client.
         *
         * @param scope the scope(s) used for the client
         * @return the {@link Builder}
         */
        public Builder scope(String... scope) {
            if (scope != null && scope.length > 0) {
                this.scopes = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(scope)));
            }
            return this;
        }

        /**
         * Sets the scope(s) used for the client.
         *
         * @param scope the scope(s) used for the client
         * @return the {@link Builder}
         */
        public Builder scope(Collection<String> scope) {
            if (scope != null && !scope.isEmpty()) {
                this.scopes = Collections.unmodifiableSet(new LinkedHashSet<>(scope));
            }
            return this;
        }

        /**
         * Builds a new {@link ClientRegistry}.
         *
         * @return a {@link ClientRegistry}
         */
        public ClientRegistry build() {
            Assert.notNull(this.authorizationGrantType, "authorizationGrantType cannot be null");
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(this.authorizationGrantType)) {
                this.validateClientCredentialsGrantType();
            }
            // else if (AuthorizationGrantType.PASSWORD.equals(this.authorizationGrantType)) {
            //     this.validatePasswordGrantType();
            // }
            else if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(this.authorizationGrantType)) {
                this.validateAuthorizationCodeGrantType();
            }
            this.validateAuthorizationGrantTypes();
            this.validateScopes();
            return this.create();
        }

        private ClientRegistry create() {
            ClientRegistry clientRegistry = new ClientRegistry();
            clientRegistry.registrationId = this.registrationId;
            clientRegistry.clientId = this.clientId;
            clientRegistry.clientSecret = StringUtils.hasText(this.clientSecret) ? this.clientSecret : "";
            clientRegistry.clientAuthenticationMethod = (this.clientAuthenticationMethod != null)
                    ? this.clientAuthenticationMethod.getValue(): deduceClientAuthenticationMethod(clientRegistry).getValue();
            clientRegistry.authorizationGrantType = this.authorizationGrantType.getValue();
            clientRegistry.redirectUri = this.redirectUri;
            clientRegistry.scopes = this.scopes;
            clientRegistry.providerDetails = createProviderDetails(clientRegistry);
            clientRegistry.clientName = StringUtils.hasText(this.clientName) ? this.clientName
                    : this.registrationId;
            return clientRegistry;
        }

        private ClientAuthenticationMethod deduceClientAuthenticationMethod(ClientRegistry clientRegistry) {
            if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(this.authorizationGrantType)
                    && !StringUtils.hasText(this.clientSecret)) {
                return ClientAuthenticationMethod.NONE;
            }
            return ClientAuthenticationMethod.CLIENT_SECRET_BASIC;
        }

        private ProviderDetails createProviderDetails(ClientRegistry clientRegistry) {
            ProviderDetails providerDetails = new ProviderDetails();
            providerDetails.authorizationUri = this.authorizationUri;
            providerDetails.tokenUri = this.tokenUri;
            providerDetails.userInfoEndpoint.uri = this.userInfoUri;
            providerDetails.userInfoEndpoint.authenticationMethod = this.userInfoAuthenticationMethod.getValue();
            providerDetails.userInfoEndpoint.userNameAttributeName = this.userNameAttributeName;
            providerDetails.jwkSetUri = this.jwkSetUri;
            providerDetails.issuerUri = this.issuerUri;
            providerDetails.configurationMetadata = Collections.unmodifiableMap(this.configurationMetadata);
            return providerDetails;
        }

        private void validateAuthorizationCodeGrantType() {
            Assert.isTrue(AuthorizationGrantType.AUTHORIZATION_CODE.equals(this.authorizationGrantType),
                    () -> "authorizationGrantType must be " + AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
            Assert.hasText(this.registrationId, "registrationId cannot be empty");
            Assert.hasText(this.clientId, "clientId cannot be empty");
            Assert.hasText(this.redirectUri, "redirectUri cannot be empty");
            Assert.hasText(this.authorizationUri, "authorizationUri cannot be empty");
            Assert.hasText(this.tokenUri, "tokenUri cannot be empty");
        }

        private void validateClientCredentialsGrantType() {
            Assert.isTrue(AuthorizationGrantType.CLIENT_CREDENTIALS.equals(this.authorizationGrantType),
                    () -> "authorizationGrantType must be " + AuthorizationGrantType.CLIENT_CREDENTIALS.getValue());
            Assert.hasText(this.registrationId, "registrationId cannot be empty");
            Assert.hasText(this.clientId, "clientId cannot be empty");
            Assert.hasText(this.tokenUri, "tokenUri cannot be empty");
        }


        private void validateAuthorizationGrantTypes() {
            for (AuthorizationGrantType authorizationGrantType : AUTHORIZATION_GRANT_TYPES) {
                if (authorizationGrantType.getValue().equalsIgnoreCase(this.authorizationGrantType.getValue())
                        && !authorizationGrantType.equals(this.authorizationGrantType)) {
                    logger.warn(LogMessage.format(
                            "AuthorizationGrantType: %s does not match the pre-defined constant %s and won't match a valid OAuth2AuthorizedClientProvider",
                            this.authorizationGrantType, authorizationGrantType));
                }
            }
        }

        private void validateScopes() {
            if (this.scopes == null) {
                return;
            }
            for (String scope : this.scopes) {
                Assert.isTrue(validateScope(scope), "scope \"" + scope + "\" contains invalid characters");
            }
        }

        private static boolean validateScope(String scope) {
            return scope == null || scope.chars().allMatch((c) -> withinTheRangeOf(c, 0x21, 0x21)
                    || withinTheRangeOf(c, 0x23, 0x5B) || withinTheRangeOf(c, 0x5D, 0x7E));
        }

        private static boolean withinTheRangeOf(int c, int min, int max) {
            return c >= min && c <= max;
        }

    }
}
