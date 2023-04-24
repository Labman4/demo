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
import com.elpsykongroo.auth.client.dto.Client;
import com.elpsykongroo.auth.client.dto.ClientRegistry;
import com.elpsykongroo.base.common.CommonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("auth/")
public class AuthController {
    @Autowired
    private AuthService authService;

    @PostMapping("/client/add")
    public String addClient (@RequestHeader("Authorization") String auth, @RequestBody Client client) {
        return CommonResponse.string(authService.addClient(auth, client));
    }
    @DeleteMapping("/client/delete")
    public String deleteClient (@RequestHeader("Authorization") String auth, @RequestParam("clientId")String clientId) {
        return CommonResponse.string(authService.deleteClient(auth, clientId));
    }
    @GetMapping("/client/list")
    public String listClient (@RequestHeader("Authorization") String auth) {
        return CommonResponse.string(authService.findAllClient(auth));
    }

    @PostMapping("/client/register/add")
    public String addRegister (@RequestHeader("Authorization") String auth, @RequestBody ClientRegistry client) {
        return CommonResponse.string(authService.addRegister(auth, client));
    }
    @DeleteMapping("/client/register/delete")
    public String deleteRegister (@RequestHeader("Authorization") String auth, @RequestParam("registerId")String clientId) {
        return CommonResponse.string(authService.deleteRegister(auth, clientId));
    }
    @GetMapping("/client/register/list")
    public String listRegister (@RequestHeader("Authorization") String auth) {
        return CommonResponse.string(authService.findAllRegister(auth));
    }
}
