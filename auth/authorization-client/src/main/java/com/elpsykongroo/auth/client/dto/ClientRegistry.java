package com.elpsykongroo.auth.client.dto;
/*
 * Copyright 2002-2022 the original author or authors.
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

import lombok.Data;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Data
public class ClientRegistry {

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

    public Builder withRegistrationId(String registrationId) {
        Assert.hasText(registrationId, "registrationId cannot be empty");
        return new Builder(registrationId);
    }

    /**
     * Returns a new {@link Builder}, initialized with the provided
     * {@link ClientRegistry}.
     *
     * @param clientRegistry the {@link ClientRegistry} to copy from
     * @return the {@link Builder}
     */
    public Builder withClientRegistration(ClientRegistry clientRegistry) {
        Assert.notNull(clientRegistry, "clientRegistry cannot be null");
        return new Builder(clientRegistry);
    }

    public static class ClientAuthenticationMethod implements Serializable {

        /**
         * @since 5.5
         */
        public static final ClientAuthenticationMethod CLIENT_SECRET_BASIC = new ClientAuthenticationMethod(
                "client_secret_basic");

        /**
         * @since 5.5
         */
        public static final ClientAuthenticationMethod CLIENT_SECRET_POST = new ClientAuthenticationMethod(
                "client_secret_post");

        /**
         * @since 5.5
         */
        public static final ClientAuthenticationMethod CLIENT_SECRET_JWT = new ClientAuthenticationMethod(
                "client_secret_jwt");

        /**
         * @since 5.5
         */
        public static final ClientAuthenticationMethod PRIVATE_KEY_JWT = new ClientAuthenticationMethod("private_key_jwt");

        /**
         * @since 5.2
         */
        public static final ClientAuthenticationMethod NONE = new ClientAuthenticationMethod("none");

        private final String value;

        /**
         * Constructs a {@code ClientAuthenticationMethod} using the provided value.
         * @param value the value of the client authentication method
         */
        public ClientAuthenticationMethod(String value) {
            Assert.hasText(value, "value cannot be empty");
            this.value = value;
        }

        /**
         * Returns the value of the client authentication method.
         * @return the value of the client authentication method
         */
        public String getValue() {
            return this.value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            ClientAuthenticationMethod that = (ClientAuthenticationMethod)obj;
            return getValue().equals(that.getValue());
        }

        @Override
        public int hashCode() {
            return getValue().hashCode();
        }

    }
    public static class AuthorizationGrantType implements Serializable {


        public static final AuthorizationGrantType AUTHORIZATION_CODE = new AuthorizationGrantType("authorization_code");

        public static final AuthorizationGrantType REFRESH_TOKEN = new AuthorizationGrantType("refresh_token");

        public static final AuthorizationGrantType CLIENT_CREDENTIALS = new AuthorizationGrantType("client_credentials");

        /**
         * @deprecated The latest OAuth 2.0 Security Best Current Practice disallows the use
         * of the Resource Owner Password Credentials grant. See reference
         * <a target="_blank" href=
         * "https://datatracker.ietf.org/doc/html/draft-ietf-oauth-security-topics-19#section-2.4">OAuth
         * 2.0 Security Best Current Practice.</a>
         */
        @Deprecated
        public static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");

        /**
         * @since 5.5
         */
        public static final AuthorizationGrantType JWT_BEARER = new AuthorizationGrantType(
                "urn:ietf:params:oauth:grant-type:jwt-bearer");

        private final String value;

        /**
         * Constructs an {@code AuthorizationGrantType} using the provided value.
         * @param value the value of the authorization grant type
         */
        public AuthorizationGrantType(String value) {
            Assert.hasText(value, "value cannot be empty");
            this.value = value;
        }

        /**
         * Returns the value of the authorization grant type.
         * @return the value of the authorization grant type
         */
        public String getValue() {
            return this.value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            AuthorizationGrantType that = (AuthorizationGrantType) obj;
            return this.getValue().equals(that.getValue());
        }

        @Override
        public int hashCode() {
            return this.getValue().hashCode();
        }

    }

    public static class AuthenticationMethod implements Serializable {

        public static final AuthenticationMethod HEADER = new AuthenticationMethod("header");

        public static final AuthenticationMethod FORM = new AuthenticationMethod("form");

        public static final AuthenticationMethod QUERY = new AuthenticationMethod("query");

        private final String value;

        /**
         * Constructs an {@code AuthenticationMethod} using the provided value.
         * @param value the value of the authentication method type
         */
        public AuthenticationMethod(String value) {
            Assert.hasText(value, "value cannot be empty");
            this.value = value;
        }

        /**
         * Returns the value of the authentication method type.
         * @return the value of the authentication method type
         */
        public String getValue() {
            return this.value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || this.getClass() != obj.getClass()) {
                return false;
            }
            AuthenticationMethod that = (AuthenticationMethod) obj;
            return this.getValue().equals(that.getValue());
        }

        @Override
        public int hashCode() {
            return this.getValue().hashCode();
        }

    }
    /**
     * Details of the Provider.
     */
    public static class ProviderDetails implements Serializable {


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


            private String uri;

            private String authenticationMethod = AuthenticationMethod.HEADER.value;

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

        private Builder(String registrationId) {
            this.registrationId = registrationId;
        }

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
         * Sets the registration id.
         *
         * @param registrationId the registration id
         * @return the {@link Builder}
         */
        public Builder registrationId(String registrationId) {
            this.registrationId = registrationId;
            return this;
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
         * Sets the client secret.
         *
         * @param clientSecret the client secret
         * @return the {@link Builder}
         */
        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        /**
         * Sets the {@link ClientAuthenticationMethod authentication method} used when
         * authenticating the client with the authorization server.
         *
         * @param clientAuthenticationMethod the authentication method used for the client
         * @return the {@link Builder}
         */
        public Builder clientAuthenticationMethod(ClientAuthenticationMethod clientAuthenticationMethod) {
            this.clientAuthenticationMethod = clientAuthenticationMethod;
            return this;
        }

        /**
         * Sets the {@link AuthorizationGrantType authorization grant type} used for the
         * client.
         *
         * @param authorizationGrantType the authorization grant type used for the client
         * @return the {@link Builder}
         */
        public Builder authorizationGrantType(AuthorizationGrantType authorizationGrantType) {
            this.authorizationGrantType = authorizationGrantType;
            return this;
        }

        /**
         * Sets the uri (or uri template) for the redirection endpoint.
         * <p>
         * <br />
         * The supported uri template variables are: {baseScheme}, {baseHost}, {basePort},
         * {basePath} and {registrationId}.
         * <p>
         * <br />
         * <b>NOTE:</b> {baseUrl} is also supported, which is the same as
         * {baseScheme}://{baseHost}{basePort}{basePath}.
         * <p>
         * <br />
         * Configuring uri template variables is especially useful when the client is
         * running behind a Proxy Server. This ensures that the X-Forwarded-* headers are
         * used when expanding the redirect-uri.
         *
         * @param redirectUri the uri (or uri template) for the redirection endpoint
         * @return the {@link Builder}
         * @since 5.4
         */
        public Builder redirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
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
         * Sets the uri for the authorization endpoint.
         *
         * @param authorizationUri the uri for the authorization endpoint
         * @return the {@link Builder}
         */
        public Builder authorizationUri(String authorizationUri) {
            this.authorizationUri = authorizationUri;
            return this;
        }

        /**
         * Sets the uri for the token endpoint.
         *
         * @param tokenUri the uri for the token endpoint
         * @return the {@link Builder}
         */
        public Builder tokenUri(String tokenUri) {
            this.tokenUri = tokenUri;
            return this;
        }

        /**
         * Sets the uri for the user info endpoint.
         *
         * @param userInfoUri the uri for the user info endpoint
         * @return the {@link Builder}
         */
        public Builder userInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
            return this;
        }

        /**
         * Sets the authentication method for the user info endpoint.
         *
         * @param userInfoAuthenticationMethod the authentication method for the user info
         *                                     endpoint
         * @return the {@link Builder}
         * @since 5.1
         */
        public Builder userInfoAuthenticationMethod(AuthenticationMethod userInfoAuthenticationMethod) {
            this.userInfoAuthenticationMethod = userInfoAuthenticationMethod;
            return this;
        }

        /**
         * Sets the attribute name used to access the user's name from the user info
         * response.
         *
         * @param userNameAttributeName the attribute name used to access the user's name
         *                              from the user info response
         * @return the {@link Builder}
         */
        public Builder userNameAttributeName(String userNameAttributeName) {
            this.userNameAttributeName = userNameAttributeName;
            return this;
        }

        /**
         * Sets the uri for the JSON Web Key (JWK) Set endpoint.
         *
         * @param jwkSetUri the uri for the JSON Web Key (JWK) Set endpoint
         * @return the {@link Builder}
         */
        public Builder jwkSetUri(String jwkSetUri) {
            this.jwkSetUri = jwkSetUri;
            return this;
        }

        /**
         * Sets the issuer identifier uri for the OpenID Connect 1.0 provider or the OAuth
         * 2.0 Authorization Server.
         *
         * @param issuerUri the issuer identifier uri for the OpenID Connect 1.0 provider
         *                  or the OAuth 2.0 Authorization Server
         * @return the {@link Builder}
         * @since 5.4
         */
        public Builder issuerUri(String issuerUri) {
            this.issuerUri = issuerUri;
            return this;
        }

        /**
         * Sets the metadata describing the provider's configuration.
         *
         * @param configurationMetadata the metadata describing the provider's
         *                              configuration
         * @return the {@link Builder}
         * @since 5.1
         */
        public Builder providerConfigurationMetadata(Map<String, Object> configurationMetadata) {
            if (configurationMetadata != null) {
                this.configurationMetadata = new LinkedHashMap<>(configurationMetadata);
            }
            return this;
        }

        /**
         * Sets the logical name of the client or registration.
         *
         * @param clientName the client or registration name
         * @return the {@link Builder}
         */
        public Builder clientName(String clientName) {
            this.clientName = clientName;
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

        private void validatePasswordGrantType() {
            Assert.isTrue(AuthorizationGrantType.PASSWORD.equals(this.authorizationGrantType),
                    () -> "authorizationGrantType must be " + AuthorizationGrantType.PASSWORD.getValue());
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
