package com.elpsykongroo.auth.server.config;

import com.elpsykongroo.auth.server.service.webauthn.RegistrationService;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class RpConfig {
    @Autowired
    Environment env;

    @Bean
    @Autowired
    public RelyingParty relyingParty(RegistrationService regisrationRepository) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(env.getProperty("HOSTNAME"))
                .name(env.getProperty("DISPLAY"))
                .build();
        String origins = env.getProperty("ORIGIN");
        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(regisrationRepository)
                .origins(Arrays.stream(origins.split(",")).toList().stream().collect(Collectors.toSet()))
                .build();
    }
}
