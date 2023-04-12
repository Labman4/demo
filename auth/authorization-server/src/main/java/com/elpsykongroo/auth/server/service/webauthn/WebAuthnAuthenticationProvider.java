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

package com.elpsykongroo.auth.server.service.webauthn;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;


@Component
public class WebAuthnAuthenticationProvider implements AuthenticationProvider {

    private AuthenticationProvider delegate; // this can be either of the above providers
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if (!supports(authentication.getClass())) {
            throw new IllegalArgumentException("Only WebAuthnAuthenticationToken is supported, " + authentication.getClass() + " was attempted");
        }
        WebAuthnAuthenticationToken webAuthnAuthenticationToken = (WebAuthnAuthenticationToken) authentication;
        if (webAuthnAuthenticationToken.isAuthenticated()) {
            authentication.setAuthenticated(true);
        } else {
            throw new BadCredentialsException("Invalid username or password");
        }
        return authentication;
//        OAuth2AccessTokenAuthenticationToken rawAuthentication = (OAuth2AccessTokenAuthenticationToken) delegate.authenticate(authentication);
//        Map<String, Object> newParameters = new HashMap<>(rawAuthentication.getAdditionalParameters());
//        rawAuthentication.getRegisteredClient().getScopes().stream().map(scope ->
//                webAuthnAuthenticationToken.getAuthorities().stream().map(authority ->
//                        newParameters.put(scope, authority.getAuthority().split(scope + ".")[1]))
//                        );
//
//        return new OAuth2AccessTokenAuthenticationToken(
//                rawAuthentication.getRegisteredClient(),
//                (Authentication)rawAuthentication.getPrincipal(),
//                rawAuthentication.getAccessToken(),
//                rawAuthentication.getRefreshToken(),
//                newParameters);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WebAuthnAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
