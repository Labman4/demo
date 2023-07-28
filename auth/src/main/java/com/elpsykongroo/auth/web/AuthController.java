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

import com.elpsykongroo.auth.service.custom.EmailService;
import com.elpsykongroo.auth.service.custom.LoginService;

import com.elpsykongroo.base.common.CommonResponse;
import com.elpsykongroo.base.domain.search.repo.AccessRecord;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@RestController
public class AuthController {
    @Autowired
    private LoginService loginService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private GatewayService gatewayService;

    @Autowired
    private EmailService emailService;

    @PostMapping("/register")
    @ResponseBody
    public String newUserRegistration(@RequestParam String username,
                                      @RequestParam String display,
                                      HttpServletRequest request) {
        saveRecord(request);
        return CommonResponse.string(loginService.register(username, display));
    }

    @PostMapping("/finishAuth")
    @ResponseBody
    public String finishRegistration(@RequestParam String credential,
                                     @RequestParam String username,
                                     @RequestParam String credname,
                                     HttpServletRequest request) {
        saveRecord(request);
        return CommonResponse.string(loginService.saveAuth(credential, username, credname));
    }

    @PostMapping("/login")
    @ResponseBody
    public String startLogin(@RequestParam String username,
                             HttpServletRequest request) {
        saveRecord(request);
        return CommonResponse.string(loginService.login(username, request));
    }

    @PostMapping("/login/token")
    @ResponseBody
    public String loginWithToken(@RequestParam String token,
                                 @RequestParam String idToken,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        saveRecord(request);
        return CommonResponse.string(loginService.loginWithToken(token, idToken, request, response));
    }

    @GetMapping("/qrcode")
    public String generateQrcode(HttpServletRequest request) {
        saveRecord(request);
        return CommonResponse.string(loginService.qrcode());
    }

    @GetMapping("/login/qrcode")
    public String setToken(@RequestParam String text, HttpServletRequest request) {
        saveRecord(request);
        return loginService.setToken(text);
    }

    @PostMapping("/access")
    public String getToken(@RequestParam String key, HttpServletRequest request) {
        saveRecord(request);
        return CommonResponse.string(redisService.getToken(key));
    }

    @PostMapping("/welcome")
    @ResponseBody
    public String finishLogin(@RequestParam String credential,
                              @RequestParam String username,
                              HttpServletRequest request, HttpServletResponse response) {
        saveRecord(request);
        return CommonResponse.string(loginService.handleLogin(credential, username, request, response));
    }

    @GetMapping("/tmp/{text}")
    public ModelAndView tmpLogin(@PathVariable String text,
                                 HttpServletRequest request, HttpServletResponse response) {
        saveRecord(request);
        return new ModelAndView(loginService.tmpLogin(text, request, response));
    }

    @PostMapping("/authenticator/add")
    public String addAuthenticator(@RequestParam String username, HttpServletRequest request) {
        saveRecord(request);
        return CommonResponse.string(loginService.addAuthenticator(username));
    }

    @PostMapping("/email/tmp")
    public void tmpLogin(@RequestParam String username,
                         HttpServletRequest request) {
        saveRecord(request);
        emailService.sendTmpLoginCert(username);
    }

    @GetMapping("/email/verify/{text}")
    public String emailVerify(@PathVariable String text,
                              HttpServletRequest request) {
        saveRecord(request);
        return CommonResponse.string(emailService.verify(text));
    }

    @PostMapping("/email/verify")
    public void sendVerify(@RequestParam String username,
                           HttpServletRequest request) {
        saveRecord(request);
        emailService.sendVerify(username);
    }

    private void saveRecord(HttpServletRequest request) {
        try {
            Map<String, String> result = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = request.getHeader(key);
                result.put(key, value);
            }
            AccessRecord record = new AccessRecord();
            record.setSourceIP(gatewayService.getIP());
            record.setRequestHeader(result);
            record.setAccessPath(request.getRequestURI());
            record.setTimestamp(Instant.now().toString());
            record.setUserAgent(request.getHeader("user-agent"));
            gatewayService.saveRecord(record);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("saveRecord error:{}", e.getMessage());
            }
        }
    }

}
