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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public interface LoginService {
    String login(String username, HttpServletRequest request);

    String handleLogin(String credential, String username, HttpServletRequest request, HttpServletResponse response);

    String saveAuth(String credential, String username, String credname);

    String register(String username, String display);

    String addAuthenticator(String username);

    String tmpLogin(String text, HttpServletRequest request, HttpServletResponse response);

    String qrcode();

    String setToken(String text);

    String loginWithToken(String token, String idToken, HttpServletRequest request, HttpServletResponse response);
}
