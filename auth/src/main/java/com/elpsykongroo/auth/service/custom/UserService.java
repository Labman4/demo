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

package com.elpsykongroo.auth.service.custom;

import com.elpsykongroo.auth.entity.user.Authority;
import com.elpsykongroo.auth.entity.user.User;
import com.elpsykongroo.auth.entity.user.UserInfo;
import com.yubico.webauthn.data.ByteArray;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface UserService extends UserDetailsService {
    @Override
    User loadUserByUsername(String username) throws UsernameNotFoundException;

    List<User> findByUsername(String username);

    int updateUserInfo(UserInfo userinfo);

    void updateUserInfoEmail(String email, String username, Map<String, Object> userInfo, Boolean emailVerified);

    int updateUser(User user);

    String loadUserInfo(String username);

    List<User> list(String pageNumber, String pageSize, String order);

    int deleteByUsername(String username);

    User add(User user);

    User findByHandle(ByteArray handle);

    List<Authority> userAuthority(String username);

    long countUser(String name);

    boolean validUser(String username, String id);

}


