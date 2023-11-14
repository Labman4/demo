/*
 * Copyright 2022 the original author or authors.
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

package com.elpsykongroo.auth.service.custom.impl;

import com.elpsykongroo.auth.entity.authorization.Authorization;
import com.elpsykongroo.auth.repository.authorization.AuthorizationRepository;
import com.elpsykongroo.auth.service.custom.AuthorizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Slf4j
public class AuthorizationServiceImpl implements AuthorizationService {
    @Autowired
    private AuthorizationRepository authorizationRepository;

    @Override
    public String getToken(String principalName) {
        List<Authorization> authorizations = authorizationRepository
                .findByPrincipalNameAndOidcIdTokenExpiresAtAfterOrderByAccessTokenIssuedAtDesc(principalName, Instant.now());
        if (log.isDebugEnabled()) {
            log.debug("validated auth size: {}", authorizations.size());
        }
        if (!authorizations.isEmpty()) {
            String accessToken = authorizations.get(0).getAccessTokenValue();
            String idToken = authorizations.get(0).getOidcIdTokenValue();
            return accessToken + "&&" + idToken;
        }
        return "";
    }
}
