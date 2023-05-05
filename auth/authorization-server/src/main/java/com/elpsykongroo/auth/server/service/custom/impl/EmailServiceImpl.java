/*
 * Copyright 2022-2022 the original author or authors.
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

package com.elpsykongroo.auth.server.service.custom.impl;

import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.repository.user.UserRepository;
import com.elpsykongroo.auth.server.service.custom.AuthenticatorService;
import com.elpsykongroo.auth.server.service.custom.EmailService;
import com.elpsykongroo.auth.server.service.custom.UserService;
import com.elpsykongroo.auth.server.utils.Random;
import com.elpsykongroo.services.redis.client.RedisService;
import com.elpsykongroo.services.redis.client.dto.KV;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;

import static com.elpsykongroo.auth.server.service.custom.impl.LoginServiceImpl.verifyChallenge;

@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private AuthenticatorService authenticatorService;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisService redisService;
    @Autowired
    private UserRepository userRepository;

    public void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@elpsykongroo.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
        } catch (MailException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String verify(String text) {
        String[] texts = text.split("\\.");
        String codeVerifier = texts[0];
        String username = texts[1];
        String encodedVerifier = verifyChallenge(codeVerifier);
        String tmp = redisService.get("email_verify_" + username);
        if (tmp.equals(encodedVerifier)) {
            User user = userService.loadUserByUsername(username);
            Map<String, Object> info = user.getUserInfo();
            userService.updateUserInfoEmail(user.getEmail(), user.getUsername(), info, true);
            return "success";
        } else {
            return "failed";
        }
    }

    @Override
    public void sendTmpLoginCert(String username) {
        String codeVerifier =  genertateVerifier();
        String codeChallenge = genertateChallenge(codeVerifier);
        KV kv = new KV("TmpCert_" + username, codeChallenge);
        redisService.set(kv);
        String email = "";
        Map<String, Object> userInfo = userService.loadUserByUsername(username).getUserInfo();
        if (userInfo.get("email") != null && "true".equals(userInfo.get("email_verified").toString())) {
            email = userInfo.get("email").toString();
        }
        send(email, "once login", "https://auth.elpsykongroo.com/tmp/" + codeVerifier + "." + username);
    }

    @Override
    public void sendVerify(String username) {
        String codeVerifier = genertateVerifier();
        String codeChallenge = genertateChallenge(codeVerifier);
        KV kv = new KV("email_verify_" + username, codeChallenge);
        redisService.set(kv);
        User user = userService.loadUserByUsername(username);
        if (StringUtils.isNotEmpty(user.getEmail())) {
            send(user.getEmail(), "verfiy email", "https://auth.elpsykongroo.com/email/verify/" + codeVerifier + "." + username);
        }
    }

    private String genertateChallenge(String codeVerifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(codeVerifier.getBytes());
            byte[] digest = md.digest();
            String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return codeChallenge;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private String genertateVerifier() {
        byte[] bytes = Random.generateRandomByte(128);
        String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return codeVerifier;
    }
}