package com.elpsykongroo.demo.controller;

import com.elpsykongroo.auth.client.AuthService;
import com.elpsykongroo.auth.client.dto.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("auth/client")
public class AuthContronller {
    @Autowired
    private AuthService authService;

    @PostMapping("/add")
    public String addClient (@RequestBody Client client) {
        return authService.add(client);
    }
    @DeleteMapping("/delete")
    public String deleteClient (@RequestParam("clientId")String clientId) {
        authService.delete(clientId);
        return "done";
    }
    @GetMapping("/list")
    public String listClient () {
        return authService.findAll();
    }
}
