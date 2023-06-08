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

package com.elpsykongroo.auth.service.custom.impl;

import com.elpsykongroo.auth.entity.user.Authenticator;
import com.elpsykongroo.auth.entity.user.User;
import com.elpsykongroo.auth.repository.user.AuthenticatorRepository;
import com.elpsykongroo.auth.service.custom.AuthenticatorService;
import com.yubico.webauthn.data.ByteArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthenticatorServiceImpl implements AuthenticatorService {

    @Autowired
    private AuthenticatorRepository authenticatorRepository;

    @Override
    public List<Authenticator> findByUser(String username) {
        return authenticatorRepository.findByUser_Username(username);
    }

    @Override
    public List<Authenticator> findByName(String name) {
        return authenticatorRepository.findByName(name);
    }

    @Override
    public long deleteByName(String name) {
       return authenticatorRepository.deleteByName(name);
    }

    @Override
    public void deleteById(String id) {
        authenticatorRepository.deleteById(id);
    }

    @Override
    public Authenticator add(Authenticator authenticator) {
        return authenticatorRepository.save(authenticator);
    }

    @Override
    public int updateCount(Long count, ByteArray credId) {
        return authenticatorRepository.updateCountByCredentialId(count + 1, credId);
    }

    @Override
    public Optional<Authenticator> findByCredentialId(ByteArray credentialId) {
        return authenticatorRepository.findByCredentialId(credentialId);
    }

    @Override
    public List<Authenticator> findAllByUser(User user) {
        return authenticatorRepository.findAllByUser(user);
    }

    @Override
    public List<Authenticator> findAllByCredentialId(ByteArray credentialId) {
        return authenticatorRepository.findAllByCredentialId(credentialId);
    }
}
