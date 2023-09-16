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


package com.elpsykongroo.base.config;

import com.elpsykongroo.base.service.GatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;



@Slf4j
@Component
public class AccessManager<T> implements AuthorizationManager<T> {
    @Autowired
    private GatewayService gatewayService;

    @Autowired
    private ServiceConfig serviceConfig;

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, T object) {
        if ("gateway".equals(serviceConfig.getSecurity())) {
            return new AuthorizationDecision(true);
        }
        String ip = gatewayService.getIP();
        if (log.isDebugEnabled()) {
            log.debug("AccessManager ip:{}", ip);
        }
        String black = gatewayService.blackOrWhite("true", ip);
        if ("true".equals(black)) {
            return new AuthorizationDecision(false);
        }
        return new AuthorizationDecision(true);
    }
}
