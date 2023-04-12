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

package com.elpsykongroo.auth.server.web;

import com.elpsykongroo.auth.server.service.custom.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @ResponseBody
    public String newUserRegistration(
        @RequestParam String username,
        @RequestParam String display
    ) {
        return userService.register(username, display);
    }

    @PostMapping("/finishauth")
    @ResponseBody
    public String finishRegisration(
            @RequestParam String credential,
            @RequestParam String username,
            @RequestParam String credname
    ) {
        return userService.saveAuth(credential, username, credname);
    }

    @PostMapping("/login")
    @ResponseBody
    public String startLogin(
        @RequestParam String username, HttpServletRequest request
    ) {
        return userService.login(username, request);
    }

    @PostMapping("/welcome")
    @ResponseBody
    public String finishLogin(
            @RequestParam String credential,
            @RequestParam String username,
            HttpServletRequest request, HttpServletResponse response) {
            return userService.handlelogin(credential, username, request, response);
    }
}
