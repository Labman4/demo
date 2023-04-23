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

import com.elpsykongroo.auth.server.entity.user.Authenticator;
import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.service.custom.AuthenticatorService;
import com.elpsykongroo.auth.server.service.custom.EmailService;
import com.elpsykongroo.auth.server.service.custom.UserService;
import com.yubico.webauthn.data.ByteArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private AuthenticatorService authenticatorService;

    @Autowired
    private UserService userService;

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
        byte[] secret = Base64.getUrlDecoder().decode(text);
        List<Authenticator> authList = authenticatorService.findAllByCredentialId(new ByteArray(secret));
        if (authList.size() == 1) {
            User user = userService.loadUserByUsername(authList.get(0).getName());
            Map<String, Object> info = user.getUserInfo();
            userService.updateUserInfoEmail(user.getEmail(), user.getUsername(), info, true);
            return "success";
        } else {
            return "failed";
        }
    }
}
