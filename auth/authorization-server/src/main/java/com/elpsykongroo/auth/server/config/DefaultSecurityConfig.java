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

package com.elpsykongroo.auth.server.config;

import com.elpsykongroo.auth.server.security.FederatedIdentityConfigurer;
import com.elpsykongroo.auth.server.security.UserRepositoryOAuth2UserHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import static org.springframework.security.config.Customizer.withDefaults;


/**
 * @author Steve Riesenberg
 * @since 0.2.3
 */
@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {
	@Autowired
    private UserDetailsService userDetailsService;

	@Bean
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		FederatedIdentityConfigurer federatedIdentityConfigurer = new FederatedIdentityConfigurer()
			.oauth2UserHandler(new UserRepositoryOAuth2UserHandler());

		http.cors().and()
			.sessionManagement()
			.sessionCreationPolicy(SessionCreationPolicy.NEVER).and()
			.csrf().disable()
//			.logout()
//			.logoutSuccessHandler(logoutSuccessHandler).and()
//			.oauth2Login().loginPage("http://localhost:15173").and()
				.oauth2Login().and()
				.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
			.authorizeHttpRequests(authorize ->
				authorize
					.requestMatchers("/oauth2/**").permitAll()
					.requestMatchers("/auth/**").permitAll()
					.requestMatchers(
							"/",
							"/index",
							"/welcome",
							"/login",
							"/register",
							"/registerauth",
							"/css/**",
							"/javascript/**",
							"/finishauth").permitAll()
					.anyRequest().authenticated()
			)
			.formLogin().disable()
			.apply(federatedIdentityConfigurer);
		return http.build();
	}

//	@Bean
//	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//		http
//			.securityMatcher("/auth/**")
//				.authorizeHttpRequests()
//					.requestMatchers("/auth/client/list").hasAuthority("SCOPE_auth.read")
//					.requestMatchers("/auth/client/add").hasAuthority("SCOPE_auth.write")
//					.requestMatchers("/auth/client/delete").hasAuthority("SCOPE_auth.write")
//					.and()
//			.oauth2ResourceServer()
//				.jwt();
//		return http.build();
//	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

//	@Bean
//	public LogoutSuccessHandler logoutSuccessHandler() {
//		return new CustomLogoutSuccessHandler();
//	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService);
	}

//	@Bean
//	public WebAuthnAuthenticationProvider AuthenticationProvider () {
//		return new WebAuthnAuthenticationProvider();
//	}
}
