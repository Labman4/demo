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
import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.base.handler.SpaCsrfTokenRequestHandler;
import com.elpsykongroo.base.optional.filter.CsrfSessionFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

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
	private ServiceConfig serviceConfig;

	@Autowired
	private AccessManager accessManager;

	@Bean
	public DefaultSecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
		requestCache.setRequestMatcher(new AntPathRequestMatcher("/oauth2/authorize/**"));
		http.cors(withDefaults())
				.csrf((csrf) -> csrf
						.csrfTokenRepository(httpSessionCsrfTokenRepository())
						.csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
						.ignoringRequestMatchers("ip", "search", "record", "message/**", "redis/**", "storage/**", "app/**")
				)
				.addFilterAfter(csrfSessionFilter(), BasicAuthenticationFilter.class)
//				.csrf(csrf -> csrf.disable())
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
						.requestMatchers(HttpMethod.GET, "/message/publicKey").permitAll()
						.requestMatchers("/notice/**").hasAuthority("admin")
						.requestMatchers(requestConfig.getPath().getPermit()).permitAll()
						.requestMatchers("/access").authenticated()
						.requestMatchers(
								"/oauth2/**",
								"/login/**",
								"/welcome",
								"/register",
								"/finishAuth",
								"/email/tmp",
								"/tmp/**").permitAll()
						.requestMatchers(HttpMethod.GET,"/email/verify/**").permitAll()
						.requestMatchers("/auth/user/authority/**").permitAll()
						.requestMatchers("/auth/user/list").hasAuthority("admin")
						.requestMatchers("/auth/user/**").authenticated()
						.requestMatchers("/auth/**").hasAuthority("admin")
						.anyRequest().access(accessManager)
				)
				.oauth2ResourceServer(rs -> rs.opaqueToken(withDefaults()))
				.requestCache(
						cache -> cache.requestCache(requestCache)
				);
		return http.build();
	}

	@Bean
	public HttpSessionCsrfTokenRepository httpSessionCsrfTokenRepository () {
		return new HttpSessionCsrfTokenRepository();
	}

	@Bean
	public CsrfSessionFilter csrfSessionFilter() {
		return new CsrfSessionFilter(httpSessionCsrfTokenRepository());
	}
}