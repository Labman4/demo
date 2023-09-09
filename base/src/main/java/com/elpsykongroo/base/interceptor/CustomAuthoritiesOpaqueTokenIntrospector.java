/*
 * Copyright 2022-2022 the original author or authors.
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

package com.elpsykongroo.base.interceptor;

import com.elpsykongroo.base.domain.auth.user.Authority;
import com.elpsykongroo.base.service.AuthService;
import com.elpsykongroo.base.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.NimbusOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CustomAuthoritiesOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
    @Value("${spring.security.oauth2.resourceserver.opaque-token.introspection-uri}")
    String introspectionUri;

    @Value("${spring.security.oauth2.resourceserver.opaque-token.client-id}")
    String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaque-token.client-secret}")
    String clientSecret;

    @Autowired
    private AuthService authService;

    @Override
    public OAuth2AuthenticatedPrincipal introspect(String token) {
        OpaqueTokenIntrospector delegate =
                new NimbusOpaqueTokenIntrospector(introspectionUri, clientId, clientSecret);
        OAuth2AuthenticatedPrincipal principal = delegate.introspect(token);
        return new DefaultOAuth2AuthenticatedPrincipal(
                principal.getName(), principal.getAttributes(), extractAuthorities(principal));
    }

    private Collection<GrantedAuthority> extractAuthorities(OAuth2AuthenticatedPrincipal principal) {
        String authorities = authService.userAllAuthority(principal.getName());
        if(log.isTraceEnabled()) {
            log.trace("authorities:{}", authorities);
        }
        List<Authority> authorityList = JsonUtils.toType(authorities, new TypeReference<List<Authority>>() {
        });
        return authorityList.stream().map(authority -> {
            return new SimpleGrantedAuthority(authority.getAuthority());
        }).collect(Collectors.toList());
    }
}