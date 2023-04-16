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

package com.elpsykongroo.auth.server.security;

import java.io.IOException;

import com.elpsykongroo.base.utils.DomainUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * An {@link AuthenticationEntryPoint} for initiating the login flow to an
 * external provider using the {@code idp} query parameter, which represents the
 * {@code registrationId} of the desired {@link ClientRegistration}.
 *
 * @author Steve Riesenberg
 * @since 0.2.3
 */
@Slf4j
public final class FederatedIdentityAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final AuthenticationEntryPoint delegate;
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	private String authorizationRequestUri = OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI
			+ "/{registrationId}";

	private final ClientRegistrationRepository clientRegistrationRepository;

	public FederatedIdentityAuthenticationEntryPoint(String loginPageUrl, ClientRegistrationRepository clientRegistrationRepository) {
		this.delegate = new LoginUrlAuthenticationEntryPoint(loginPageUrl);
		this.clientRegistrationRepository = clientRegistrationRepository;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) throws IOException, ServletException {
		String challenge = request.getParameter("code_challenge");
		String query = request.getQueryString();
		String redirect = request.getParameter("redirect_uri");
		String state = request.getParameter("state");
		if (redirect != null && state != null && StringUtils.isBlank(challenge)) {
			String parent = DomainUtils.getParentDomain(redirect);
			String subDomain = DomainUtils.getSubDomain(redirect);
			if (StringUtils.isNotBlank(subDomain)) {
				ClientRegistration clientRegistration = this.clientRegistrationRepository.findByRegistrationId(subDomain);
				if (clientRegistration != null) {
					log.debug("match idp");
					this.redirectStrategy.sendRedirect(request, response, "https://" + parent + "?" + query);
					return;
				}
			} else {
				log.debug("not match idp");
				this.redirectStrategy.sendRedirect(request, response, "https://" + parent + "?" + query);
				return;
			}
		}
		String idp = request.getParameter("idp");
		if (idp != null) {
			ClientRegistration clientRegistration = this.clientRegistrationRepository.findByRegistrationId(idp);
			if (clientRegistration != null) {
				String redirectUri = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request))
						.replaceQuery(null)
						.replacePath(this.authorizationRequestUri)
						.buildAndExpand(clientRegistration.getRegistrationId())
						.toUriString();
				this.redirectStrategy.sendRedirect(request, response, redirectUri);
				return;
			}
		}

		this.delegate.commence(request, response, authenticationException);
	}

	public void setAuthorizationRequestUri(String authorizationRequestUri) {
		this.authorizationRequestUri = authorizationRequestUri;
	}

}
