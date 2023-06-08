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

package com.elpsykongroo.base.domain.auth.client;

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
import java.util.LinkedHashSet;
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

            private String authenticationMethod;

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
