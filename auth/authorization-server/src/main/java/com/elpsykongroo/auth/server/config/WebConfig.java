package com.elpsykongroo.auth.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/auth/**");
                registry.addMapping("/login");
                registry.addMapping("/logout");
                registry.addMapping("/oauth2/**").allowedOriginPatterns("*").allowCredentials(true);
                registry.addMapping("/register");
                registry.addMapping("/userinfo").allowedOriginPatterns("*").allowCredentials(true);
            }
        };
    }


//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowedOriginPatterns(Arrays.asList("*"));
//        config.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE"));
//        config.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
//        config.addAllowedHeader("*");
//        config.addAllowedMethod("*");
//        config.addAllowedOrigin("http://127.0.0.1:15173");
//        config.addAllowedOrigin("https://elpsykongroo.com");
//        config.addAllowedOrigin("https://login.elpsykongroo.com");
//        config.addAllowedOrigin("https://auth-dev.elpsykongroo.com");
//        config.addAllowedOrigin("https://oauth2-proxy2.elpsykongroo.com");
//        config.setAllowCredentials(true);
//        source.registerCorsConfiguration("/**", config);
//        return source;
//    }

    @Bean
    public DefaultCookieSerializer defaultCookieSerializer() {
        DefaultCookieSerializer defaultCookieSerializer = new DefaultCookieSerializer();
        defaultCookieSerializer.setCookiePath("/");
        defaultCookieSerializer.setSameSite(null);
        defaultCookieSerializer.setDomainNamePattern("^(([^.]+)\\.)?(elpsykongroo\\.com|localhost|127.0.0.1)$");
        return defaultCookieSerializer;
    }
}
