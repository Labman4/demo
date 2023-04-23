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

package com.elpsykongroo.auth.client.impl;

import com.elpsykongroo.auth.client.AuthService;
import com.elpsykongroo.auth.client.dto.Client;
import com.elpsykongroo.auth.client.dto.ClientRegistry;
import com.elpsykongroo.auth.client.dto.User;
import com.elpsykongroo.auth.client.dto.UserInfo;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthServiceImpl implements AuthService {

    private RestTemplate restTemplate;

    private String serverUrl = "http://localhost:9000";

    private String authorityPrefix =  "/auth/authority";

    private String groupPrefix =  "/auth/group";

    private String userPrefix =  "/auth/user";

    private String clientPrefix =  "/auth/client";

    private String registryPrefix =  "/auth/client/register";

    public AuthServiceImpl(String serverUrl, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.serverUrl = serverUrl;
    }
    @Override
    public String addClient(String auth, Client client) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<Client> requestEntity = new HttpEntity(client, headers);
        return restTemplate.exchange(serverUrl + clientPrefix + "/add",
                HttpMethod.POST,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String deleteClient(String auth, String clientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + clientPrefix + "/delete/" + clientId,
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String findAllClient(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + clientPrefix + "/list",
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();
//        return restTemplate.getForEntity(serverUrl + servicePrefix + "/list", String.class).getBody();
    }

    @Override
    public String addRegister(String auth, ClientRegistry client) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<Client> requestEntity = new HttpEntity(client, headers);
        return restTemplate.exchange(serverUrl + registryPrefix + "/add",
                HttpMethod.POST,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String deleteRegister(String auth, String clientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + registryPrefix + "/delete/" + clientId,
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();    }

    @Override
    public String findAllRegister(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + registryPrefix + "/list",
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String addGroup(String auth, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + groupPrefix + "/add?group=" +  name,
                HttpMethod.PUT,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String addAuthority(String auth, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + authorityPrefix + "/add?name=" +  name,
                HttpMethod.PUT,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String authorityList(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + authorityPrefix + "/list" ,
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String groupList(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + groupPrefix + "/list" ,
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String deleteGroup(String auth, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + groupPrefix + "/delete/" +  name,
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String deleteAuthority(String auth, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + authorityPrefix + "/delete/" +  name,
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String loadUserInfo(String auth, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(
                serverUrl + userPrefix + "/info"
                        + "?username=" + username,
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String updateUserInfo(String auth, UserInfo userinfo) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<UserInfo> requestEntity = new HttpEntity(userinfo, headers);
        return restTemplate.exchange(serverUrl + userPrefix + "/info/patch",
                HttpMethod.PATCH,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String updateUser(String auth, User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<User> requestEntity = new HttpEntity(user, headers);
        return restTemplate.exchange(serverUrl + userPrefix + "/patch",
                HttpMethod.PATCH,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String updateUserGroup(String auth, String groups, String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        MultiValueMap<String, String> ids = new LinkedMultiValueMap<>();
        ids.add("groups", groups);
        ids.add("ids", id);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity(ids,headers);
        return restTemplate.exchange(serverUrl + groupPrefix + "/user/patch",
                HttpMethod.PATCH,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String updateUserAuthority(String auth, String authorities, String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        MultiValueMap<String, String> ids = new LinkedMultiValueMap<>();
        ids.add("authorities", authorities);
        ids.add("ids", id);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity(ids,headers);
        return restTemplate.exchange(serverUrl + authorityPrefix + "/user/patch",
                HttpMethod.PATCH,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String updateGroupAuthority(String auth, String authorities, String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        MultiValueMap<String, String> ids = new LinkedMultiValueMap<>();
        ids.add("authorities", authorities);
        ids.add("ids", id);
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity(ids,headers);
        return restTemplate.exchange(serverUrl + authorityPrefix + "/group/patch",
                HttpMethod.PATCH,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String userList(String auth, String pageNumber, String pageSize, String order) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(
                serverUrl + userPrefix + "/list"
                        + "?pageNumber=" + pageNumber
                        + "&pageSize=" + pageSize
                        + "&order=" + order,
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String userAuthority(String auth, String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(
                serverUrl + authorityPrefix + "/user/list"
                        + "?id=" + id,
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();    }

    @Override
    public String userGroup(String auth, String id) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(
                serverUrl + groupPrefix + "/user/list"
                        + "?id=" + id,
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();        }

    @Override
    public String groupAuthorityList(String auth, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(
                serverUrl + groupPrefix + "/authority/list"
                        + "?name=" + name,
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();        }

    @Override
    public String authorityGroupList(String auth, String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(
                serverUrl + authorityPrefix + "/group/list"
                        + "?name=" + name,
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();        }
}
