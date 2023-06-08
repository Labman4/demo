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

package com.elpsykongroo.auth.service.client;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReactiveJpaClientRegistrationRepository implements ReactiveClientRegistrationRepository {
    private final Map<String, ClientRegistration> clientIdToClientRegistration;

    public ReactiveJpaClientRegistrationRepository(List<ClientRegistration> registrations) {
        this.clientIdToClientRegistration = toUnmodifiableConcurrentMap(registrations);
    }

    @Override
    public Mono<ClientRegistration> findByRegistrationId(String registrationId) {
        return Mono.justOrEmpty((ClientRegistration)this.clientIdToClientRegistration.get(registrationId));
    }

    private static Map<String, ClientRegistration> toUnmodifiableConcurrentMap(List<ClientRegistration> registrations) {
        Assert.notEmpty(registrations, "registrations cannot be null or empty");
        ConcurrentHashMap<String, ClientRegistration> result = new ConcurrentHashMap();
        Iterator var2 = registrations.iterator();

        while (var2.hasNext()) {
            ClientRegistration registration = (ClientRegistration) var2.next();
            Assert.notNull(registration, "no registration can be null");
            if (result.containsKey(registration.getRegistrationId())) {
                throw new IllegalStateException(String.format("Duplicate key %s", registration.getRegistrationId()));
            }

            result.put(registration.getRegistrationId(), registration);
        }

        return Collections.unmodifiableMap(result);
    }
}
