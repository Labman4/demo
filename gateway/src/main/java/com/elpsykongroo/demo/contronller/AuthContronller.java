package com.elpsykongroo.demo.contronller;

import com.elpsykongroo.auth.client.AuthService;
import com.elpsykongroo.auth.client.dto.Client;
import com.elpsykongroo.auth.client.dto.ClientRegistry;
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
public class AuthContronller {
    @Autowired
    private AuthService authService;

    @PostMapping("/client/add")
    public String addClient (@RequestHeader("Authorization") String auth, @RequestBody Client client) {
        return authService.addClient(auth, client);
    }
    @DeleteMapping("/client/delete")
    public String deleteClient (@RequestHeader("Authorization") String auth, @RequestParam("clientId")String clientId) {
        return authService.deleteClient(auth, clientId);
    }
    @GetMapping("/client/list")
    public String listClient (@RequestHeader("Authorization") String auth) {
        return authService.findAllClient(auth);
    }

    @PostMapping("/register/add")
    public String addRegister (@RequestHeader("Authorization") String auth, @RequestBody ClientRegistry client) {
        return authService.addRegister(auth, client);
    }
    @DeleteMapping("/register/delete")
    public String deleteRegister (@RequestHeader("Authorization") String auth, @RequestParam("registerId")String clientId) {
        return authService.deleteRegister(auth, clientId);
    }
    @GetMapping("/register/list")
    public String listRegister (@RequestHeader("Authorization") String auth) {
        return authService.findAllRegister(auth).toString();
    }
}
