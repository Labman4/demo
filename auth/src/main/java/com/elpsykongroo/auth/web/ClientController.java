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

import com.elpsykongroo.auth.entity.client.Client;
import com.elpsykongroo.auth.entity.client.ClientRegistry;
import com.elpsykongroo.auth.service.custom.ClientRegistrationService;
import com.elpsykongroo.auth.service.custom.ClientService;
import com.elpsykongroo.base.common.CommonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/auth/client")
public class ClientController {
    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientRegistrationService clientRegistrationService;

    @PutMapping("/register")
    public String addClientRegistry(@RequestBody ClientRegistry client) {
        return CommonResponse.data(clientRegistrationService.add(client));
    }
    @DeleteMapping("/register/{registerId}")
    public String deleteClientRegistry(@PathVariable String registerId) {
        return CommonResponse.data(clientRegistrationService.delete(registerId));
    }
    @GetMapping("/register")
    public String listClientRegistry() {
        return CommonResponse.data(clientRegistrationService.findAll());
    }
    @PutMapping
    public String addClient(@RequestBody Client client) {
        return CommonResponse.data(clientService.add(client));
    }

    @DeleteMapping("/{clientId}")
    public String deleteClient(@PathVariable String clientId) {
        return CommonResponse.string(clientService.delete(clientId));
    }

    @GetMapping
    public String listClient() {
        return CommonResponse.data(clientService.findAll());
    }
}
