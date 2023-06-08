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

package com.elpsykongroo.base.service;

import com.elpsykongroo.base.domain.auth.client.Client;
import com.elpsykongroo.base.domain.auth.client.ClientRegistry;
import com.elpsykongroo.base.domain.auth.user.User;
import com.elpsykongroo.base.domain.auth.user.UserInfo;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

public interface AuthService {
    @RequestLine("PUT /auth/client")
    @Headers({
            "Content-Type: application/json"
    })
    String addClient(Client client);

    @RequestLine("DELETE /auth/client/{clientId}")
    String deleteClient(@Param String clientId);

    @RequestLine("GET /auth/client")
    String findAllClient();

    @RequestLine("PUT /auth/client/register")
    @Headers({
            "Content-Type: application/json"
    })
    String addRegister(ClientRegistry client);

    @RequestLine("DELETE /auth/client/register/{clientId}")
    String deleteRegister(String clientId);

    @RequestLine("GET /auth/client/register")
    String findAllRegister();

    @RequestLine("PUT /auth/group/name={name}")
    String addGroup(@Param String name);

    @RequestLine("GET /auth/group")
    String groupList();

    @RequestLine("DELETE /auth/group/{name}")
    String deleteGroup(@Param String name);

    @RequestLine("PUT /auth/authority/name={name}")
    String addAuthority(@Param String name);

    @RequestLine("GET /auth/authority")
    String authorityList();

    @RequestLine("DELETE /auth/authority/{name}")
    String deleteAuthority(@Param String name);

    @RequestLine("GET /auth/user/info/{username}")
    String loadUserInfo(String username);

    @RequestLine("PATCH /auth/user/info")
    @Headers({
            "Content-Type: application/json"
    })
    String updateUserInfo(UserInfo userinfo);

    @RequestLine("PATCH /auth/user")
    @Headers({
            "Content-Type: application/json"
    })
    String updateUser(User user);

    @RequestLine("PATCH /auth/group/user?groups={groups}&ids={ids}")
    String updateUserGroup(@Param String groups, @Param String ids);

    @RequestLine("PATCH /auth/authority/user?authorities={authorities}&ids={ids}")
    String updateUserAuthority(@Param String authorities, @Param String id);

    @RequestLine("PATCH /auth/authority/group?authorities={authorities}&ids={ids}")
    String updateGroupAuthority(@Param String authorities, @Param String ids);

    @RequestLine("GET /auth/user?pageNumber={pageNumber}&pageSize={pageSize}&order={order}")
    String userList(@Param String pageNumber, @Param String pageSize, @Param String order);

    @RequestLine("GET /auth/authority/user/{id}")
    String userAuthority(@Param String id);
    @RequestLine("GET /auth/user/authority/{username}")
    String userAllAuthority(@Param String username);

    @RequestLine("GET /auth/group/user/{id}")
    String userGroup(@Param String id);

    @RequestLine("GET /auth/authority/group/{name}")
    String groupAuthorityList(@Param String name);

    @RequestLine("GET /auth/authority/group/{name}")
    String authorityGroupList(@Param String name);

    @RequestLine("GET /auth/user/{username}")
    String loadUser(@Param String username);
}
