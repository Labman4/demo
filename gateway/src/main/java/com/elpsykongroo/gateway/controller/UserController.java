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

import com.elpsykongroo.auth.client.AuthService;
import com.elpsykongroo.auth.client.dto.User;
import com.elpsykongroo.auth.client.dto.UserInfo;
import com.elpsykongroo.base.common.CommonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/auth")
public class UserController {
    @Autowired
    private AuthService authService;

    @GetMapping("/user/list")
    public String userList (@RequestHeader("Authorization") String auth,
                              @RequestParam String pageNumber,
                              @RequestParam String pageSize,
                              @RequestParam String order) {
        return CommonResponse.string(authService.userList(auth, pageNumber, pageSize, order));
    }

    @GetMapping("/user/info")
    public String userinfo (@RequestHeader("Authorization") String auth,
                            @RequestParam String username) {
        return CommonResponse.string(authService.loadUserInfo(auth, username));
    }

    @PatchMapping("/user/patch")
    public String userPatch (@RequestHeader("Authorization") String auth,
                             @RequestBody User user) {
        return CommonResponse.string(authService.updateUser(auth, user));
    }

    @PatchMapping("/user/info/patch")
    public String userInfoPatch (@RequestHeader("Authorization") String auth,
                            @RequestBody UserInfo userinfo) {
        return CommonResponse.string(authService.updateUserInfo(auth, userinfo));
    }

    @GetMapping("/group/authority/list")
    public String groupAuthorityList (@RequestHeader("Authorization") String auth,
                             @RequestParam String name) {
        return CommonResponse.string(authService.groupAuthorityList(auth, name));
    }

    @GetMapping("/authority/group/list")
    public String authorityGroupList (@RequestHeader("Authorization") String auth,
                             @RequestParam String name) {
        return CommonResponse.string(authService.authorityGroupList(auth, name));
    }

    @GetMapping("/group/list")
    public String groupList (@RequestHeader("Authorization") String auth) {
        return CommonResponse.string(authService.groupList(auth));
    }

    @GetMapping("/authority/list")
    public String authorityList (@RequestHeader("Authorization") String auth) {
        return CommonResponse.string(authService.authorityList(auth));
    }

    @PutMapping("/group/add")
    public String addGroup (@RequestHeader("Authorization") String auth,
                            @RequestParam String name) {
        return CommonResponse.string(authService.addGroup(auth, name));
    }

    @PutMapping("/authority/add")
    public String addAuthority (@RequestHeader("Authorization") String auth,
                            @RequestParam String name) {
        return CommonResponse.string(authService.addAuthority(auth, name));
    }

    @DeleteMapping("/group/delete")
    public String deleteGroup (@RequestHeader("Authorization") String auth,
                                @RequestParam("name") String name) {
        return CommonResponse.string(authService.deleteGroup(auth, name));
    }

    @DeleteMapping("/authority/delete")
    public String deleteAuthority (@RequestHeader("Authorization") String auth,
                                   @RequestParam("name") String name) {
        return CommonResponse.string(authService.deleteAuthority(auth, name));
    }

    @PatchMapping("/group/user/patch")
    public String updateGroup (@RequestHeader("Authorization") String auth,
                               @RequestParam("groups") String groups,
                               @RequestParam("ids") String ids) {
        return CommonResponse.string(authService.updateUserGroup(auth, groups, ids));
    }

    @PatchMapping("/authority/user/patch")
    public String updateAuthority (@RequestHeader("Authorization") String auth,
                               @RequestParam("authorities") String authorities,
                               @RequestParam("ids") String ids) {
        return CommonResponse.string(authService.updateUserAuthority(auth, authorities, ids));
    }

    @PatchMapping("/authority/group/patch")
    public String updateGroupAuthority (@RequestHeader("Authorization") String auth,
                               @RequestParam("authorities") String authorities,
                               @RequestParam("ids") String ids) {
        return CommonResponse.string(authService.updateGroupAuthority(auth, authorities, ids));
    }

    @GetMapping("/authority/user/list")
    public String userAuthorityList(
            @RequestHeader("Authorization") String auth,
            @RequestParam String id) {
        return CommonResponse.string(authService.userAuthority(auth, id));
    }

    @GetMapping("/user/authority/list")
    public String allAuthorityList(
            @RequestHeader("Authorization") String auth,
            @RequestParam("username") String username) {
        return CommonResponse.data(authService.userAllAuthority(auth, username));
    }

    @GetMapping("/group/user/list")
    public String userGroupList(
            @RequestHeader("Authorization") String auth,
            @RequestParam String id) {
        return CommonResponse.string(authService.userGroup(auth, id));
    }

    @GetMapping("/user/{username}")
    public String user(
            @RequestHeader("Authorization") String auth,
            @PathVariable String username) {
        return CommonResponse.string(authService.loadUser(auth, username));
    }
}
