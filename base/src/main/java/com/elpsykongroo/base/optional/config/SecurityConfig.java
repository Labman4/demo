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

package com.elpsykongroo.base.optional.config;

import com.elpsykongroo.base.config.AccessManager;
import com.elpsykongroo.base.config.RequestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.resource.OAuth2ResourceServerConfigurer;
import org.springframework.security.web.DefaultSecurityFilterChain;
//
//@ConditionalOnProperty(
//		prefix = "service",
//		name = "security",
//		havingValue = "gateway",
//		matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {
	@Autowired
	private RequestConfig requestConfig;

	@Autowired
	private AccessManager accessManager;

	@Bean
	public DefaultSecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.cors().and()
				.csrf().disable()
 //				.requiresChannel(channel ->
//						channel.anyRequest().requiresSecure())
				.authorizeHttpRequests((authorize) -> authorize
 						.requestMatchers("/public/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/actuator/**").permitAll()
						.requestMatchers(HttpMethod.GET,"/storage/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/ip").permitAll()
						.requestMatchers(HttpMethod.POST, "/search").permitAll()
						.requestMatchers(HttpMethod.PUT, "/notice/register").permitAll()
						.requestMatchers(HttpMethod.GET, "/notice/user").permitAll()
						.requestMatchers("/notice/**").hasAuthority("admin")
						.requestMatchers(requestConfig.getPath().getPermit()).permitAll()
						.anyRequest().access(accessManager)
				)
				.oauth2ResourceServer(OAuth2ResourceServerConfigurer::opaqueToken);
		return http.build();
	}
}