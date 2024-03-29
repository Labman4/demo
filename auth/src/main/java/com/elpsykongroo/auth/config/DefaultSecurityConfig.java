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

import com.elpsykongroo.base.config.ServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class DefaultSecurityConfig {
	@Autowired
    private UserDetailsService userDetailsService;

	@Autowired
	private ServiceConfig serviceConfig;

	@Bean
	public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
		http
			.formLogin(f -> f.disable())
			.logout((logout) -> logout
					.clearAuthentication(true)
					.invalidateHttpSession(true)
					.addLogoutHandler(new SecurityContextLogoutHandler())
					);
		http
				.sessionManagement( s ->
								s.sessionCreationPolicy(SessionCreationPolicy.NEVER)
										.maximumSessions(1)
//							.maxSessionsPreventsLogin(true)
				);
		http.oauth2ResourceServer(rs -> rs.opaqueToken(withDefaults()));
		HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
		requestCache.setRequestMatcher(new AntPathRequestMatcher("/oauth2/authorize/**"));
		http.httpBasic((basic) -> basic
						.addObjectPostProcessor(new ObjectPostProcessor<BasicAuthenticationFilter>() {
							@Override
							public <O extends BasicAuthenticationFilter> O postProcess(O filter) {
								filter.setSecurityContextRepository(new HttpSessionSecurityContextRepository());
								return filter;
							}
						}))
				.cors(withDefaults())
				.requestCache(
					cache -> cache.requestCache(requestCache)
				)
				.exceptionHandling(exceptionHandling ->
						exceptionHandling.authenticationEntryPoint(authenticationEntryPoint())
				);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(userDetailsService);
	}

	@Bean
	public HttpSessionRequestCache httpSessionRequestCache() {
		return new HttpSessionRequestCache();
	}

	@Bean
	public AuthenticationEntryPoint authenticationEntryPoint() {
		return new LoginUrlAuthenticationEntryPoint(serviceConfig.getUrl().getLoginPage());
	}
}
