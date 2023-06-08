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

package com.elpsykongroo.auth.server.repository.user;

import com.elpsykongroo.auth.server.entity.user.Authenticator;
import com.elpsykongroo.auth.server.entity.user.User;
import com.yubico.webauthn.data.ByteArray;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthenticatorRepository extends JpaRepository<Authenticator, String> {

    @Transactional
    @Modifying
    @Query("update Authenticator a set a.count = ?1 where a.credentialId = ?2")
    int updateCountByCredentialId(Long count, ByteArray credentialId);

    @Transactional
    long deleteByName(String name);

    List<Authenticator> findByUser_Username(String username);

    List<Authenticator> findByName(String name);

    Optional<Authenticator> findByCredentialId(ByteArray credentialId);

    List<Authenticator> findAllByUser (User user);

    List<Authenticator> findAllByCredentialId(ByteArray credentialId);

}
