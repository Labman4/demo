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

package com.elpsykongroo.auth.server.service.custom;

import com.elpsykongroo.auth.server.entity.user.Authenticator;
import com.elpsykongroo.auth.server.entity.user.User;
import com.yubico.webauthn.data.ByteArray;

import java.util.List;
import java.util.Optional;

public interface AuthenticatorService {
    List<Authenticator> findByUser(String username);

    List<Authenticator> findByName(String name);

    long deleteByName(String name);

    void deleteById(String id);

    Authenticator add(Authenticator authenticator);

    int updateCount(Long count, ByteArray credId);

    Optional<Authenticator> findByCredentialId(ByteArray credentialId);

    List<Authenticator> findAllByUser (User user);

    List<Authenticator> findAllByCredentialId(ByteArray credentialId);

}
