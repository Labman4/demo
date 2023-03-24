package com.elpsykongroo.auth.server.service.webauthn;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class WebAuthnAuthenticationProvider implements AuthenticationProvider {

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
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return WebAuthnAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
