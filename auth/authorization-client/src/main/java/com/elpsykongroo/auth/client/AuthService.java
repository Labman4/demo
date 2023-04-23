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

package com.elpsykongroo.auth.client;

import com.elpsykongroo.auth.client.dto.Client;
import com.elpsykongroo.auth.client.dto.ClientRegistry;
import com.elpsykongroo.auth.client.dto.User;
import com.elpsykongroo.auth.client.dto.UserInfo;


public interface AuthService {
    String addClient(String auth, Client client);

    String deleteClient(String auth, String clientId);

    String findAllClient(String auth);

    String addRegister(String auth, ClientRegistry client);

    String deleteRegister(String auth, String clientId);

    String findAllRegister(String auth);

    String addGroup(String auth, String name);

    String addAuthority(String auth, String name);

    String authorityList(String auth);

    String groupList(String auth);

    String deleteGroup(String auth, String name);

    String deleteAuthority(String auth, String name);

    String loadUserInfo(String auth, String username);

    String updateUserInfo(String auth, UserInfo userinfo);

    String updateUser(String auth, User user);

    String updateUserGroup(String auth, String groups, String ids);

    String updateUserAuthority(String auth, String authorities, String id);

    String updateGroupAuthority(String auth, String authorities, String id);

    String userList(String auth, String pageNumber, String pageSize, String order);

    String userAuthority(String auth, String id);

    String userGroup(String auth, String id);

    String groupAuthorityList(String auth, String name);

    String authorityGroupList(String auth, String name);

}
