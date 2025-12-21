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

import com.elpsykongroo.auth.service.custom.AppService;
import com.elpsykongroo.auth.service.custom.EmailService;
import com.elpsykongroo.base.common.CommonResponse;
import com.elpsykongroo.base.service.GatewayService;
import com.elpsykongroo.base.service.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
@RequestMapping("/app")
public class AppController {
    @Autowired
    private AppService appService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private GatewayService gatewayService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    @ResponseBody
    public String newUserRegistration(@RequestParam String username,
                                      @RequestParam String display) {
        return CommonResponse.string(appService.register(username, display));
    }

    @PostMapping("/finishAuth")
    @ResponseBody
    public String finishRegistration(@RequestParam String credential,
                                     @RequestParam String username,
                                     @RequestParam String credname) {
        return CommonResponse.string(appService.saveAuth(credential, username, credname));
    }

    @PostMapping("/login")
    @ResponseBody
    public String startLogin(@RequestParam String username,
                             HttpServletRequest request,
                             HttpServletResponse response) {
        return CommonResponse.string(appService.login(username, request, response));
    }

    @GetMapping("/login/tmp/{text}")
    public ModelAndView tmpLogin(@PathVariable String text,
                                 HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView(appService.tmpLogin(text, request, response));
    }

    @PostMapping("/login/token")
    @ResponseBody
    public String loginWithToken(@RequestParam String token,
                                 @RequestParam String idToken,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        return CommonResponse.string(appService.loginWithToken(token, idToken, request, response));
    }

    @GetMapping("/login/qrcode")
    public String setToken(@RequestParam String text) {
        return appService.setToken(text);
    }

    @PostMapping("/access")
    public String getToken(@RequestParam String key) {
        return CommonResponse.string(redisService.getToken(key));
    }

    @PostMapping("/welcome")
    @ResponseBody
    public String finishLogin(@RequestParam String credential,
                              @RequestParam String username,
                              HttpServletRequest request, HttpServletResponse response) {
        return CommonResponse.string(appService.handleLogin(credential, username, request, response));
    }

    @PostMapping("/authenticator/add")
    public String addAuthenticator(@RequestParam String username) {
        return CommonResponse.string(appService.addAuthenticator(username));
    }

    @PostMapping("/email/tmp")
    public void tmpLogin(@RequestParam String username) {
        emailService.sendTmpLoginCert(username);
    }

    @GetMapping("/email/verify/{text}")
    public String emailVerify(@PathVariable String text) {
        return CommonResponse.string(emailService.verify(text));
    }

    @PostMapping("/email/verify")
    public void sendVerify(@RequestParam String username) {
        emailService.sendVerify(username);
    }
}
