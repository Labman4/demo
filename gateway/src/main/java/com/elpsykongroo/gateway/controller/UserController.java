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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
    public String userlist (@RequestHeader("Authorization") String auth,
                              @RequestParam String pageNumber,
                              @RequestParam String pageSize,
                              @RequestParam String order) {
        return authService.userList(auth, pageNumber, pageSize, order);
    }

    @GetMapping("/user/info")
    public String userinfo (@RequestHeader("Authorization") String auth,
                            @RequestParam String username) {
        return authService.loadUserInfo(auth, username);
    }

    @PatchMapping("/user/patch")
    public String userlist (@RequestHeader("Authorization") String auth,
                           @RequestBody User user) {
        return authService.updateUser(auth, user);
    }

    @PatchMapping("/user/info/patch")
    public String userlist (@RequestHeader("Authorization") String auth,
                            @RequestBody UserInfo userinfo
                            ) {
        return authService.updateUserInfo(auth, userinfo);
    }

    @GetMapping("/group/authority/list")
    public String groupAuthorityList (@RequestHeader("Authorization") String auth,
                             @RequestParam String name
    ) {
        return authService.groupAuthorityList(auth, name);
    }

    @GetMapping("/authority/group/list")
    public String authorityGroupList (@RequestHeader("Authorization") String auth,
                             @RequestParam String name
    ) {
        return authService.authorityGroupList(auth, name);
    }

    @GetMapping("/group/list")
    public String groupList (@RequestHeader("Authorization") String auth
    ) {
        return authService.groupList(auth);
    }



    @GetMapping("/authority/list")
    public String authorityList (@RequestHeader("Authorization") String auth
    ) {
        return authService.authorityList(auth);
    }

    @PutMapping("/group/add")
    public String addGroup (@RequestHeader("Authorization") String auth,
                            @RequestParam String name
    ) {
        return authService.addGroup(auth, name);
    }

    @PutMapping("/authority/add")
    public String addAuthority (@RequestHeader("Authorization") String auth,
                            @RequestParam String name
    ) {
        return authService.addAuthority(auth, name);
    }

    @DeleteMapping("/group/delete")
    public String deleteGroup (@RequestHeader("Authorization") String auth,
                                @RequestParam("name") String name
    ) {
        return authService.deleteGroup(auth, name);
    }

    @DeleteMapping("/authority/delete")
    public String deleteAuthority (@RequestHeader("Authorization") String auth,
                                   @RequestParam("name") String name
    ) {
        return authService.deleteAuthority(auth, name);
    }

    @PatchMapping("/group/user/patch")
    public String updateGroup (@RequestHeader("Authorization") String auth,
                               @RequestParam("groups") String groups,
                               @RequestParam("ids") String ids
    ) {
        return authService.updateUserGroup(auth, groups, ids);
    }

    @PatchMapping("/authority/user/patch")
    public String updateAuthority (@RequestHeader("Authorization") String auth,
                               @RequestParam("authorities") String authorities,
                               @RequestParam("ids") String ids
    ) {
        return authService.updateUserAuthority(auth, authorities, ids);
    }

    @PatchMapping("/authority/group/patch")
    public String updateGroupAuthority (@RequestHeader("Authorization") String auth,
                               @RequestParam("authorities") String authorities,
                               @RequestParam("ids") String ids
    ) {
        return authService.updateGroupAuthority(auth, authorities, ids);
    }

    @GetMapping("/authority/user/list")
    public String userAuthorityList(
            @RequestHeader("Authorization") String auth,
            @RequestParam String id
    ) {
        return authService.userAuthority(auth, id);
    }

    @GetMapping("/group/user/list")
    public String userGroupList(
            @RequestHeader("Authorization") String auth,
            @RequestParam String id
    ) {
        return authService.userGroup(auth, id);
    }

}
