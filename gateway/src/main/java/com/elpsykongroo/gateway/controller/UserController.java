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

package com.elpsykongroo.gateway.controller;

import com.elpsykongroo.base.common.CommonResponse;
import com.elpsykongroo.base.domain.auth.user.User;
import com.elpsykongroo.base.domain.auth.user.UserInfo;
import com.elpsykongroo.base.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("auth")
public class UserController {
    @Autowired
    private AuthService authService;

    @GetMapping("user")
    public String userList (@RequestParam String pageNumber,
                            @RequestParam String pageSize,
                            @RequestParam String order) {
        return CommonResponse.string(authService.userList(pageNumber, pageSize, order));
    }

    @GetMapping("user/info/{username}")
    public String userinfo (@PathVariable String username) {
        return CommonResponse.string(authService.loadUserInfo(username));
    }

    @PostMapping("user")
    public String userPatch (@RequestBody User user) {
        return CommonResponse.string(authService.updateUser(user));
    }

    @PostMapping("user/info")
    public String userInfoPatch (@RequestBody UserInfo userinfo) {
        return CommonResponse.string(authService.updateUserInfo(userinfo));
    }

    @GetMapping("group/authority/{name}")
    public String groupAuthorityList (@PathVariable String name) {
        return CommonResponse.string(authService.groupAuthorityList(name));
    }

    @GetMapping("authority/group/{name}")
    public String authorityGroupList (@PathVariable String name) {
        return CommonResponse.string(authService.authorityGroupList(name));
    }

    @GetMapping("group")
    public String groupList () {
        return CommonResponse.string(authService.groupList());
    }

    @GetMapping("authority")
    public String authorityList () {
        return CommonResponse.string(authService.authorityList());
    }

    @PutMapping("group")
    public String addGroup (@RequestParam String name) {
        return CommonResponse.string(authService.addGroup(name));
    }

    @PutMapping("authority")
    public String addAuthority (@RequestParam String name) {
        return CommonResponse.string(authService.addAuthority(name));
    }

    @DeleteMapping("group/{name}")
    public String deleteGroup (@PathVariable String name) {
        return CommonResponse.string(authService.deleteGroup(name));
    }

    @DeleteMapping("authority/{name}")
    public String deleteAuthority (@PathVariable String name) {
        return CommonResponse.string(authService.deleteAuthority(name));
    }

    @PostMapping("group/user")
    public String updateGroup (@RequestParam String groups,
                               @RequestParam String ids) {
        return CommonResponse.string(authService.updateUserGroup(groups, ids));
    }

    @PostMapping("authority/user")
    public String updateAuthority (@RequestParam String authorities,
                                   @RequestParam String ids) {
        return CommonResponse.string(authService.updateUserAuthority(authorities, ids));
    }

    @PostMapping("authority/group")
    public String updateGroupAuthority (@RequestParam String authorities,
                                        @RequestParam String ids) {
        return CommonResponse.string(authService.updateGroupAuthority(authorities, ids));
    }

    @GetMapping("authority/user/{id}")
    public String userAuthorityList(@PathVariable String id) {
        return CommonResponse.string(authService.userAuthority(id));
    }

    @GetMapping("user/authority/{username}")
    public String allAuthorityList(@PathVariable String username) {
        return CommonResponse.string(authService.userAllAuthority(username));
    }

    @GetMapping("group/user/{id}")
    public String userGroupList(@PathVariable String id) {
        return CommonResponse.string(authService.userGroup(id));
    }

    @GetMapping("user/{username}")
    public String user(@PathVariable String username) {
        return CommonResponse.string(authService.loadUser(username));
    }
}
