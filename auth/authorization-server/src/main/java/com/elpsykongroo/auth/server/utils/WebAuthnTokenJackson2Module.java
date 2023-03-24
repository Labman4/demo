package com.elpsykongroo.auth.server.utils;

import com.elpsykongroo.auth.server.service.webauthn.WebAuthnAuthenticationToken;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.security.jackson2.SecurityJackson2Modules;


public class WebAuthnTokenJackson2Module extends SimpleModule {
    public WebAuthnTokenJackson2Module() {
        super(com.elpsykongroo.auth.server.utils.WebAuthnTokenJackson2Module.class.getName(), new Version(1, 0, 0, (String)null, (String)null, (String)null));
    }

    public void setupModule(Module.SetupContext context) {
        SecurityJackson2Modules.enableDefaultTyping((ObjectMapper)context.getOwner());
        context.setMixInAnnotations(WebAuthnAuthenticationToken.class, WebAuthnTokenMixin.class);
    }
}