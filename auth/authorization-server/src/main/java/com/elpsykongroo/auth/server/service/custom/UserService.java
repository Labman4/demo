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

package com.elpsykongroo.auth.server.service.custom;

import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.entity.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService extends UserDetailsService {
    User loadUserByUsername(String username) throws UsernameNotFoundException;

    String login(String username, HttpServletRequest request);

    String handlelogin(String credential, String username, HttpServletRequest request, HttpServletResponse response);

    String saveAuth(String credential, String username, String credname);

    String register(String username, String display);

    int updateUserInfo(UserInfo userinfo);

    int updateUser(User user);

    String loadUserInfo(String username);

    List<User> list(String pageNumber, String pageSize, String order);

}


