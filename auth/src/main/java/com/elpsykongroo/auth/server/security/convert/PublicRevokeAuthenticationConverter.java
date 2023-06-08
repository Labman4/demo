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

package com.elpsykongroo.auth.server.security.convert;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenRevocationAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.authentication.AuthenticationConverter;
@Slf4j
public class PublicRevokeAuthenticationConverter implements AuthenticationConverter {
    public PublicRevokeAuthenticationConverter(RegisteredClientRepository repository) {
        this.repository = repository;
    }
    @Autowired
    private RegisteredClientRepository repository;
    @Override
    public Authentication convert(HttpServletRequest request) {
        String token = request.getParameter("token");
        String tokenType = request.getParameter("token_type_hint");
        String clientId = request.getParameter("client_id");
        if (token != null && clientId != null) {
            log.debug("public revoke");
            RegisteredClient registeredClient = repository.findByClientId(clientId);
            if (registeredClient != null ) {
                OAuth2ClientAuthenticationToken client =
                        new OAuth2ClientAuthenticationToken(registeredClient,
                                ClientAuthenticationMethod.NONE, null);
                OAuth2TokenRevocationAuthenticationToken revoke =
                        new OAuth2TokenRevocationAuthenticationToken(
                                request.getParameter("token"),
                                client,
                                tokenType != null ? tokenType : "");
                revoke.setAuthenticated(true);
                return revoke;
            }
        }
        return null;
    }
}
