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

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class WebAuthnAuthenticationToken extends AbstractAuthenticationToken {

    public static WebAuthnAuthenticationToken unauthenticated(Object principal, Object credentials) {
        return new WebAuthnAuthenticationToken(principal, credentials);
    }

    public static WebAuthnAuthenticationToken authenticated(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        return new WebAuthnAuthenticationToken(principal, credentials, authorities);
    }
    private Object credentials;

    private Object principal;

    public WebAuthnAuthenticationToken(Object principal, Object credentials) {
        super((Collection)null);
        this.principal = principal;
        this.credentials = credentials;
        this.setAuthenticated(false);
    }

    public WebAuthnAuthenticationToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.credentials = credentials;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return this.credentials;
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }

    public void eraseCredentials() {
        super.eraseCredentials();
        this.credentials = null;
    }
}
