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
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.oauth2.core.AuthenticationMethod;

import java.io.Serializable;
import java.util.Collections;
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

    public static class ProviderDetails implements Serializable {

        private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;

        private String authorizationUri;

        private String tokenUri;

        private UserInfoEndpoint userInfoEndpoint = new UserInfoEndpoint();

        private String jwkSetUri;

        private String issuerUri;

        private Map<String, Object> configurationMetadata = Collections.emptyMap();

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
}
