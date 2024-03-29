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

package com.elpsykongroo.auth.security;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.elpsykongroo.auth.entity.user.Authority;
import com.elpsykongroo.auth.entity.user.Group;
import com.elpsykongroo.auth.entity.user.OidcInfo;
import com.elpsykongroo.auth.entity.user.User;
import com.elpsykongroo.auth.service.custom.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public final class FederatedIdentityIdTokenCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

	@Autowired
	private UserService userService;

	private static final Set<String> ID_TOKEN_CLAIMS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			IdTokenClaimNames.ISS,
			IdTokenClaimNames.SUB,
			IdTokenClaimNames.AUD,
			IdTokenClaimNames.EXP,
			IdTokenClaimNames.IAT,
			IdTokenClaimNames.AUTH_TIME,
			IdTokenClaimNames.NONCE,
			IdTokenClaimNames.ACR,
			IdTokenClaimNames.AMR,
			IdTokenClaimNames.AZP,
			IdTokenClaimNames.AT_HASH,
			IdTokenClaimNames.C_HASH
	)));

	@Override
	public void customize(JwtEncodingContext context) {
		if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
			User user = userService.loadUserByUsername(context.getPrincipal().getName());
			List<Authority> authorityList = user.getAuthorities();
			List<String> groups = new ArrayList<>();
			for (Group group : user.getGroups()) {
				groups.add(group.getGroupName());
				authorityList.addAll(group.getAuthorities());
			}
			Map<String, Object> customClaims = new HashMap<>();
			Map<String, Object> info = user.getUserInfo();
			OidcUserInfo userInfo;
			if (info != null) {
				userInfo = new OidcUserInfo(info);
			} else {
				userInfo = null;
			}
			if (userInfo != null) {
				for (String scope : context.getAuthorizedScopes()) {
					List<String> authList = new ArrayList<>();
					List<String> auths = new ArrayList<>();
					for (GrantedAuthority authority : authorityList) {
						auths.add(authority.getAuthority());
						String[] auth = authority.getAuthority().split(scope + ".");
						if (authority.getAuthority().equals(scope)) {
							customClaims.put(scope, null);
						}
						if (auth.length > 1) {
							log.debug("scope:{}, auth:{}", scope, auth);
							authList.add(auth[1]);
						}
						if ("group".equals(authority.getAuthority())) {
							info.put("group", groups);
						}
					}
					if (!authList.isEmpty()) {
						info.put(scope, authList);
					}
					if (customClaims.containsKey("permission")) {
						info.put("permission", auths.stream().distinct().collect(Collectors.toList()));
					}
					userInfo = new OidcUserInfo(info);
				}
				for (Field field : OidcInfo.class.getDeclaredFields()) {
					for (String claim: userInfo.getClaims().keySet()) {
						if (field.getName().equals(claim)) {
							addClaim(context, claim, userInfo);
						}
						if (customClaims.containsKey(claim)) {
							addClaim(context, claim, userInfo);
						}
					}
				}
			}
			Map<String, Object> thirdPartyClaims = extractClaims(context.getPrincipal());

			context.getClaims().claims(existingClaims -> {
				// Remove conflicting claims set by this authorization server
				existingClaims.keySet().forEach(thirdPartyClaims::remove);
				// Remove standard id_token claims that could cause problems with clients
				ID_TOKEN_CLAIMS.forEach(thirdPartyClaims::remove);

				// Add all other claims directly to id_token
				existingClaims.putAll(thirdPartyClaims);
			});
		}
	}

	private Map<String, Object> extractClaims(Authentication principal) {
		Map<String, Object> claims;
		if (principal.getPrincipal() instanceof OidcUser) {
			OidcUser oidcUser = (OidcUser) principal.getPrincipal();
			OidcIdToken idToken = oidcUser.getIdToken();
			claims = idToken.getClaims();
		} else if (principal.getPrincipal() instanceof OAuth2User) {
			OAuth2User oauth2User = (OAuth2User) principal.getPrincipal();
			claims = oauth2User.getAttributes();
		} else {
			claims = Collections.emptyMap();
		}
		return new HashMap<>(claims);
	}

	private void addClaim(JwtEncodingContext context, String claim, OidcUserInfo userInfo) {
		Object info =  userInfo.getClaims().get(claim);
		if (info != null && !info.toString().isBlank()) {
			context.getClaims().claims(claims -> claims.put(claim, info));
		}
	}
}
