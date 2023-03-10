package com.elpsykongroo.auth.server.web;


import com.elpsykongroo.auth.server.entity.client.ClientRegistry;
import com.elpsykongroo.auth.server.service.custom.ClientRegistrationService;
import com.elpsykongroo.auth.server.utils.JsonUtils;
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
@RequestMapping("/auth/register")
public class RegisterController {
    @Autowired
    private ClientRegistrationService clientService;

    @PostMapping("/add")
    public String addClient (@RequestBody ClientRegistry client) {
        return clientService.add(client);
    }
    @DeleteMapping("/delete/{registerId}")
    public String deleteClient (@PathVariable String registerId) {
       return clientService.delete(registerId);
    }
    @GetMapping("/list")
    public String listClient () {
        List<ClientRegistry> clientList = clientService.findAll();
        return JsonUtils.toJson(clientList);
    }
}
