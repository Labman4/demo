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


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import com.elpsykongroo.auth.server.security.CustomLogoutSuccessHandler;

import com.elpsykongroo.auth.server.security.FederatedIdentityConfigurer;
import com.elpsykongroo.auth.server.security.UserRepositoryOAuth2UserHandler;


/**
 * @author Steve Riesenberg
 * @since 0.2.3
 */
@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {
	
	@Autowired
    private UserDetailsService userDetailsService;

	@Autowired
	LogoutSuccessHandler logoutSuccessHandler;

	@Bean
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		FederatedIdentityConfigurer federatedIdentityConfigurer = new FederatedIdentityConfigurer()
			.oauth2UserHandler(new UserRepositoryOAuth2UserHandler());
		http.cors().and()
			// .csrf().disable()
			.logout()
			.logoutSuccessHandler(logoutSuccessHandler).and()
			.oauth2Login(oauth2Login ->
					oauth2Login.loginPage("/oauth2/authorization/spring"))
			.authorizeHttpRequests(authorize ->
				authorize
					.requestMatchers("/**").permitAll()
					.anyRequest().authenticated()
			)
			.formLogin(Customizer.withDefaults())
			.apply(federatedIdentityConfigurer);

		return http.build();
	}
		
	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	public LogoutSuccessHandler logoutSuccessHandler() {
		return new CustomLogoutSuccessHandler();
	}
	
	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) 
	  throws Exception {
		auth.userDetailsService(userDetailsService);
	}
}
