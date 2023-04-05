package com.elpsykongroo.auth.server.web;

import com.elpsykongroo.auth.server.service.custom.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
public class AuthController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    @ResponseBody
    public String newUserRegistration(
        @RequestParam String username,
        @RequestParam String display
    ) {
        return userService.register(username, display);
    }

    @PostMapping("/finishauth")
    @ResponseBody
    public String finishRegisration(
            @RequestParam String credential,
            @RequestParam String username,
            @RequestParam String credname
    ) {
        return userService.saveAuth(credential, username, credname);
    }

    @PostMapping("/login")
    @ResponseBody
    public String startLogin(
        @RequestParam String username, HttpServletRequest request
    ) {
        return userService.login(username, request);
    }

    @PostMapping("/welcome")
    @ResponseBody
    public String finishLogin(
            @RequestParam String credential,
            @RequestParam String username,
            HttpServletRequest request, HttpServletResponse response) {
            return userService.handlelogin(credential, username, request, response);
    }
}
