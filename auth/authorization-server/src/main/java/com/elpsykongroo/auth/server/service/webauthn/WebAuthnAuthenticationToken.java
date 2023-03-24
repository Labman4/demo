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
