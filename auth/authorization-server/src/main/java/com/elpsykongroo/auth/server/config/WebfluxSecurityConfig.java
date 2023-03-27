package com.elpsykongroo.auth.server.config;

import com.elpsykongroo.auth.server.security.PkceOAuth2AuthorizationRequestResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.web.server.SecurityWebFilterChain;

@EnableWebFluxSecurity
public class WebfluxSecurityConfig {
    @Bean
    SecurityWebFilterChain springSecurityFilter(ServerHttpSecurity http, PkceOAuth2AuthorizationRequestResolver resolver) {
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
        http.cors().and().csrf().disable()
                .oauth2Client()
                .authorizationRequestResolver(resolver);
        // @formatter:on
        return  http.build();
    }
}
