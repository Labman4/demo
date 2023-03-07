package com.elpsykongroo.demo.contronller;

import com.elpsykongroo.auth.client.AuthService;
import com.elpsykongroo.auth.client.dto.Client;
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
@RequestMapping("auth/client")
public class AuthContronller {
    @Autowired
    private AuthService authService;

    @PostMapping("/add")
    public String addClient (@RequestHeader("Authorization") String auth, @RequestBody Client client) {
        return authService.add(auth, client);
    }
    @DeleteMapping("/delete")
    public String deleteClient (@RequestHeader("Authorization") String auth, @RequestParam("clientId")String clientId) {
        return authService.delete(auth, clientId);
    }
    @GetMapping("/list")
    public String listClient (@RequestHeader("Authorization") String auth) {
        return authService.findAll(auth);
    }
}
