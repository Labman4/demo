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

import com.elpsykongroo.auth.security.FederatedIdentityAuthenticationEntryPoint;
import com.elpsykongroo.auth.security.convert.PublicRevokeAuthenticationConverter;
import com.elpsykongroo.auth.security.provider.WebAuthnAuthenticationProvider;
import com.elpsykongroo.auth.utils.jose.Jwks;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthenticatedPrincipalOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;


@Configuration(proxyBeanMethods = false)
public class AuthorizationServerConfig {
	@Value("${ISSUER_URL}")
	private String issuerUrl;

	@Autowired
	RegisteredClientRepository registeredClientRepository;

	@Autowired
	private FederatedIdentityAuthenticationEntryPoint authenticationEntryPoint;

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
		OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
		OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = http.getConfigurer(OAuth2AuthorizationServerConfigurer.class);
		http.oauth2ResourceServer(rs -> rs.opaqueToken(withDefaults()));
		RequestMatcher endpointsMatcher = authorizationServerConfigurer.getEndpointsMatcher();
//		Function<OidcUserInfoAuthenticationContext, OidcUserInfo> userInfoMapper = (context) -> {
//			OidcUserInfoAuthenticationToken authentication = context.getAuthentication();
//			JwtAuthenticationToken principal = (JwtAuthenticationToken) authentication.getPrincipal();
//			return new OidcUserInfo(principal.getToken().getClaims());
//		};

//		OAuth2ClientAuthorizationRequestResolver resolver = new OAuth2ClientAuthorizationRequestResolver(clientRegistrationRepository);
//		resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

		authorizationServerConfigurer
				.oidc(withDefaults())
//				.oidc((oidc) -> oidc
//						.userInfoEndpoint((userInfo) -> userInfo
//								.userInfoMapper(userInfoMapper)
//						)
//						.providerConfigurationEndpoint(Customizer.withDefaults())
//						.clientRegistrationEndpoint(Customizer.withDefaults())
//				)
				.authorizationEndpoint(withDefaults())
				.clientAuthentication(clientAuthentication ->
						clientAuthentication
								.authenticationConverter(new PublicRevokeAuthenticationConverter(registeredClientRepository))
								.authenticationProvider(new WebAuthnAuthenticationProvider()))
				.tokenRevocationEndpoint(tokenRevocationEndpoint ->
						tokenRevocationEndpoint
								.revocationRequestConverter(new PublicRevokeAuthenticationConverter(registeredClientRepository))
				);

//				.oauth2Client()
//					.authorizationCodeGrant()
//					.authorizationRequestResolver(resolver);

		http.apply(authorizationServerConfigurer);
		http
			.securityMatcher(endpointsMatcher)
				.sessionManagement( s ->
								s.sessionCreationPolicy(SessionCreationPolicy.NEVER)
										.maximumSessions(1)
//							.maxSessionsPreventsLogin(true)
				);
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
						exceptionHandling.authenticationEntryPoint(authenticationEntryPoint)
				);
		return http.build();
	}

	@Bean
	public JWKSource<SecurityContext> jwkSource() {
		RSAKey rsaKey = Jwks.generateRsa();
		JWKSet jwkSet = new JWKSet(rsaKey);
		return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
	}

	@Bean
	public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
		return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
	}

	@Bean
	public AuthorizationServerSettings authorizationServerSettings() {
		return AuthorizationServerSettings.builder().issuer(issuerUrl).build();
	}

	@Bean
	public OAuth2AuthorizedClientService authorizedClientService(
			JdbcTemplate jdbcTemplate,
			ClientRegistrationRepository clientRegistrationRepository) {
		return new JdbcOAuth2AuthorizedClientService(jdbcTemplate, clientRegistrationRepository);
	}

	@Bean
	public OAuth2AuthorizedClientRepository authorizedClientRepository(
			OAuth2AuthorizedClientService authorizedClientService) {
		return new AuthenticatedPrincipalOAuth2AuthorizedClientRepository(authorizedClientService);
	}

	@Bean
	OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clientRegistrationRepository,
														  OAuth2AuthorizedClientRepository authorizedClientRepository) {
		OAuth2AuthorizedClientProvider authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder
				.builder()
				.clientCredentials()
				.authorizationCode()
				.refreshToken()
				.build();
		DefaultOAuth2AuthorizedClientManager authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(clientRegistrationRepository, authorizedClientRepository);
		authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);
		return authorizedClientManager;
	}

	@Bean
	public SessionRegistry sessionRegistry() {
		return new SessionRegistryImpl();
	}

	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}

}
