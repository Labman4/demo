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

package com.elpsykongroo.auth.web;

import com.elpsykongroo.auth.entity.user.User;
import com.elpsykongroo.auth.entity.user.UserInfo;
import com.elpsykongroo.auth.service.custom.UserService;
import com.elpsykongroo.base.common.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/{username}")
    public String user(
            @PathVariable String username) {
        return CommonResponse.string(userService.loadUserByUsername(username).toString());
    }

    @GetMapping
    public String userList(
            @RequestParam String pageNumber,
            @RequestParam String pageSize,
            @RequestParam String order) {
        return CommonResponse.string(userService.list(pageNumber, pageSize, order).toString());
    }

    @GetMapping("/info/{username}")
    public String loadUserInfo(
            @PathVariable String username) {
        return CommonResponse.string(userService.loadUserInfo(username));
    }

    @PostMapping
    public String updateUser(
            @RequestBody User user) {
        return CommonResponse.data(userService.updateUser(user));
    }

    @PostMapping("/info")
    public String updateUserInfo(
            @RequestBody UserInfo userInfo) {
        return CommonResponse.data(userService.updateUserInfo(userInfo));
    }

    @GetMapping("/authority/{username}")
    public String authorityList(@PathVariable String username) {
        return CommonResponse.data(userService.userAuthority(username));
    }
}
