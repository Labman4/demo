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

import com.elpsykongroo.auth.server.entity.client.Client;
import com.elpsykongroo.auth.server.entity.client.ClientRegistry;
import com.elpsykongroo.auth.server.service.custom.ClientRegistrationService;
import com.elpsykongroo.auth.server.service.custom.ClientService;
import com.elpsykongroo.base.utils.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/auth/client")
public class ClientController {
    @Autowired
    private ClientService clientService;

    @Autowired
    private ClientRegistrationService clientRegistrationService;

    @PostMapping("/register/add")
    public String addClientRegistry (@RequestBody ClientRegistry client) {
        return clientRegistrationService.add(client);
    }
    @DeleteMapping("/register/delete/{registerId}")
    public String deleteClientRegistry (@PathVariable String registerId) {
        return clientRegistrationService.delete(registerId);
    }
    @GetMapping("/register/list")
    public String listClientRegistry () {
        List<ClientRegistry> clientList = clientRegistrationService.findAll();
        return JsonUtils.toJson(clientList);
    }
    @PostMapping("/add")
    public String addClient (@RequestBody Client client) {
        return clientService.add(client);
    }

    @DeleteMapping("/delete/{clientId}")
    public String deleteClient (@PathVariable String clientId) {
        return clientService.delete(clientId);
    }

    @GetMapping("/list")
    public String listClient () {
        List<Client> clientList = clientService.findAll();
        return JsonUtils.toJson(clientList);
    }
}
