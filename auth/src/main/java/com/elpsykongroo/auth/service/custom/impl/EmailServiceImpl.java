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

package com.elpsykongroo.auth.service.custom.impl;

import com.elpsykongroo.auth.entity.user.User;
import com.elpsykongroo.auth.service.custom.EmailService;
import com.elpsykongroo.auth.service.custom.UserService;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.utils.PkceUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Map;

import static com.elpsykongroo.auth.service.custom.impl.LoginServiceImpl.verifyChallenge;

@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisService redisService;

    @Override
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
        String[] texts = text.split("\\.", 2);
        String codeVerifier = texts[0];
        String username = texts[1];
        String encodedVerifier = verifyChallenge(codeVerifier);
        String tmp = redisService.get("email_verify_" + username);
        if (tmp.equals(encodedVerifier)) {
            User user = userService.loadUserByUsername(username);
            Map<String, Object> info = user.getUserInfo();
            redisService.set("email_verify_" + username, "", "");
            userService.updateUserInfoEmail(user.getEmail(), user.getUsername(), info, true);
            return "success";
        } else {
            return "failed";
        }
    }

    @Override
    public void sendTmpLoginCert(String username) {
        Map<String, Object> userInfo = userService.loadUserByUsername(username).getUserInfo();
        if (userInfo != null && !userInfo.isEmpty()) {
            if (userInfo.get("email") != null && "true".equals(userInfo.get("email_verified").toString())) {
                String codeVerifier = PkceUtils.generateVerifier();
                String codeChallenge = PkceUtils.generateChallenge(codeVerifier);
                redisService.set("TmpCert_" + username, codeChallenge, "");
                send(userInfo.get("email").toString(), "once login", "https://auth.elpsykongroo.com/tmp/" + codeVerifier + "." + username);
            }
        }
    }

    @Override
    public void sendVerify(String username) {
        User user = userService.loadUserByUsername(username);
        if (StringUtils.isNotEmpty(user.getEmail())) {
            String codeVerifier = PkceUtils.generateVerifier();
            String codeChallenge = PkceUtils.generateChallenge(codeVerifier);
            redisService.set("email_verify_" + username, codeChallenge, "");
            send(user.getEmail(), "verfiy email", "https://auth.elpsykongroo.com/email/verify/" + codeVerifier + "." + username);
        }
    }
}
