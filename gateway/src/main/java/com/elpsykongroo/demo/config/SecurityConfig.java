/*
 * Copyright 2022-2022 the original author or authors.
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

package com.elpsykongroo.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
	@Value("${request.path.permit}")
	private String permit_path;

	@Bean
	public DefaultSecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors().and()
		     .csrf().disable()
 //				.requiresChannel(channel ->
//						channel.anyRequest().requiresSecure())
				.authorizeHttpRequests((authorize) -> authorize
						.requestMatchers(permit_path).permitAll()
						.requestMatchers(HttpMethod.GET, "/public/*").permitAll()
//						.requestMatchers(HttpMethod.GET, "/record/**").hasAuthority("SCOPE_message:read")
//						.requestMatchers(HttpMethod.POST, "/ip/manager/*").hasAuthority("SCOPE_message:write")
						.anyRequest().authenticated()
				)
				.oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt);
		return http.build();
	}
}