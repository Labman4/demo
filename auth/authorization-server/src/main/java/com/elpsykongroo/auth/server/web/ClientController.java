package com.elpsykongroo.auth.server.web;


import com.elpsykongroo.auth.server.entity.client.Client;
import com.elpsykongroo.auth.server.service.custom.ClientService;
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
@RequestMapping("/auth/client")
public class ClientController {
    @Autowired
    private ClientService clientService;

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
