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

package com.elpsykongroo.auth.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class PkceOAuth2AuthorizationRequestResolver implements ServerOAuth2AuthorizationRequestResolver {
    public static final String DEFAULT_REGISTRATION_ID_URI_VARIABLE_NAME = "registrationId";
    public static final String DEFAULT_AUTHORIZATION_REQUEST_PATTERN = "/oauth2/authorization/{registrationId}";
    private static final char PATH_DELIMITER = '/';
    private static final StringKeyGenerator DEFAULT_STATE_GENERATOR = new Base64StringKeyGenerator(Base64.getUrlEncoder());
    private static final StringKeyGenerator DEFAULT_SECURE_KEY_GENERATOR = new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);
    private static final Consumer<OAuth2AuthorizationRequest.Builder> DEFAULT_PKCE_APPLIER = OAuth2AuthorizationRequestCustomizers.withPkce();
    private final ServerWebExchangeMatcher authorizationRequestMatcher;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer;

    public PkceOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this(clientRegistrationRepository, new PathPatternParserServerWebExchangeMatcher("/oauth2/authorization/{registrationId}"));
    }

    public PkceOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository, ServerWebExchangeMatcher authorizationRequestMatcher) {
        this.authorizationRequestCustomizer = (customizer) -> {
        };
        Assert.notNull(clientRegistrationRepository, "clientRegistrationRepository cannot be null");
        Assert.notNull(authorizationRequestMatcher, "authorizationRequestMatcher cannot be null");
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.authorizationRequestMatcher = authorizationRequestMatcher;
    }

    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange) {
        return this.authorizationRequestMatcher.matches(exchange).filter((matchResult) -> {
            return matchResult.isMatch();
        }).map(ServerWebExchangeMatcher.MatchResult::getVariables).map((variables) -> {
            return variables.get("registrationId");
        }).cast(String.class).flatMap((clientRegistrationId) -> {
            return this.resolve(exchange, clientRegistrationId);
        });
    }

    public Mono<OAuth2AuthorizationRequest> resolve(ServerWebExchange exchange, String clientRegistrationId) {
        return this.findByRegistrationId(exchange, clientRegistrationId).map((clientRegistration) -> {
            return this.authorizationRequest(exchange, clientRegistration);
        });
    }

    public final void setAuthorizationRequestCustomizer(Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer) {
        Assert.notNull(authorizationRequestCustomizer, "authorizationRequestCustomizer cannot be null");
        this.authorizationRequestCustomizer = authorizationRequestCustomizer;
    }

    private Mono<ClientRegistration> findByRegistrationId(ServerWebExchange exchange, String clientRegistration) {
        return Mono.just(this.clientRegistrationRepository.findByRegistrationId(clientRegistration));
    }

    private OAuth2AuthorizationRequest authorizationRequest(ServerWebExchange exchange, ClientRegistration clientRegistration) {
        OAuth2AuthorizationRequest.Builder builder = this.getBuilder(clientRegistration);
        String redirectUriStr = expandRedirectUri(exchange.getRequest(), clientRegistration);
        builder.clientId(clientRegistration.getClientId()).authorizationUri(clientRegistration.getProviderDetails().getAuthorizationUri()).redirectUri(redirectUriStr).scopes(clientRegistration.getScopes()).state(DEFAULT_STATE_GENERATOR.generateKey());
        this.authorizationRequestCustomizer.accept(builder);
        return builder.build();
    }

    private OAuth2AuthorizationRequest.Builder getBuilder(ClientRegistration clientRegistration) {
        if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(clientRegistration.getAuthorizationGrantType())) {
            OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode().attributes((attrs) -> {
                attrs.put("registration_id", clientRegistration.getRegistrationId());
            });
            if (!CollectionUtils.isEmpty(clientRegistration.getScopes()) && clientRegistration.getScopes().contains("openid")) {
                applyNonce(builder);
            }

            if (ClientAuthenticationMethod.NONE.equals(clientRegistration.getClientAuthenticationMethod())) {
                DEFAULT_PKCE_APPLIER.accept(builder);
            }

            return builder;
        } else {
            String var10002 = clientRegistration.getAuthorizationGrantType().getValue();
            throw new IllegalArgumentException("Invalid Authorization Grant Type (" + var10002 + ") for Client Registration with Id: " + clientRegistration.getRegistrationId());
        }
    }

    private static String expandRedirectUri(ServerHttpRequest request, ClientRegistration clientRegistration) {
        Map<String, String> uriVariables = new HashMap();
        uriVariables.put("registrationId", clientRegistration.getRegistrationId());
        UriComponents uriComponents = UriComponentsBuilder.fromUri(request.getURI()).replacePath(request.getPath().contextPath().value()).replaceQuery((String)null).fragment((String)null).build();
        String scheme = uriComponents.getScheme();
        uriVariables.put("baseScheme", scheme != null ? scheme : "");
        String host = uriComponents.getHost();
        uriVariables.put("baseHost", host != null ? host : "");
        int port = uriComponents.getPort();
        uriVariables.put("basePort", port == -1 ? "" : ":" + port);
        String path = uriComponents.getPath();
        if (StringUtils.hasLength(path) && path.charAt(0) != '/') {
            path = "/" + path;
        }

        uriVariables.put("basePath", path != null ? path : "");
        uriVariables.put("baseUrl", uriComponents.toUriString());
        String action = "";
        if (AuthorizationGrantType.AUTHORIZATION_CODE.equals(clientRegistration.getAuthorizationGrantType())) {
            action = "login";
        }

        uriVariables.put("action", action);
        return UriComponentsBuilder.fromUriString(clientRegistration.getRedirectUri()).buildAndExpand(uriVariables).toUriString();
    }

    private static void applyNonce(OAuth2AuthorizationRequest.Builder builder) {
        try {
            String nonce = DEFAULT_SECURE_KEY_GENERATOR.generateKey();
            String nonceHash = createHash(nonce);
            builder.attributes((attrs) -> {
                attrs.put("nonce", nonce);
            });
            builder.additionalParameters((params) -> {
                params.put("nonce", nonceHash);
            });
        } catch (NoSuchAlgorithmException var3) {
        }

    }

    private static String createHash(String value) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(value.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }
}
