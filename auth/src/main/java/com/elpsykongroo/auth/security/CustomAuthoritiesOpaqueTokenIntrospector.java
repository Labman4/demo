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

package com.elpsykongroo.auth.security;

import com.elpsykongroo.auth.entity.user.Authority;
import com.elpsykongroo.auth.entity.user.Group;
import com.elpsykongroo.auth.entity.user.User;
import com.elpsykongroo.auth.service.custom.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.NimbusOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomAuthoritiesOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
    @Value("${spring.security.oauth2.resourceserver.opaque-token.introspection-uri}")
    String introspectionUri;

    @Value("${spring.security.oauth2.resourceserver.opaque-token.client-id}")
    String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaque-token.client-secret}")
    String clientSecret;

    @Autowired
    private UserService userService;

    public OAuth2AuthenticatedPrincipal introspect(String token) {
        OpaqueTokenIntrospector delegate =
                new NimbusOpaqueTokenIntrospector(introspectionUri, clientId, clientSecret);
        OAuth2AuthenticatedPrincipal principal = delegate.introspect(token);
        return new DefaultOAuth2AuthenticatedPrincipal(
                principal.getName(), principal.getAttributes(), extractAuthorities(principal));
    }

    private Collection<GrantedAuthority> extractAuthorities(OAuth2AuthenticatedPrincipal principal) {
        User user = userService.loadUserByUsername(principal.getName());
        List<Authority> authorities = new ArrayList<>();
        for (Group group : user.getGroups()) {
            authorities.addAll(group.getAuthorities());
        }
        authorities.addAll(user.getAuthorities());
        return authorities.stream().map(authority -> {
                   return new SimpleGrantedAuthority(authority.getAuthority());
                }).collect(Collectors.toList());

    }
}