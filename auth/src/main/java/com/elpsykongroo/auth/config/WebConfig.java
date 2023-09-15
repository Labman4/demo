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

package com.elpsykongroo.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/auth/**").allowedOriginPatterns("*");
                registry.addMapping("/logout").allowedOriginPatterns("*").allowCredentials(true);
                registry.addMapping("/connect/logout").allowedOriginPatterns("*").allowCredentials(true);
                registry.addMapping("/oauth2/**").allowedOriginPatterns("*").allowCredentials(true);
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

//    @Bean
//    public DefaultCookieSerializer defaultCookieSerializer() {
//        DefaultCookieSerializer defaultCookieSerializer = new DefaultCookieSerializer();
//        defaultCookieSerializer.setCookiePath("/");
//        defaultCookieSerializer.setSameSite(null);
//        defaultCookieSerializer.setDomainNamePattern("^(([^.]+)\\.)?(elpsykongroo\\.com|localhost|127.0.0.1|auth.|auth-pre.)$");
//        return defaultCookieSerializer;
//    }
}
