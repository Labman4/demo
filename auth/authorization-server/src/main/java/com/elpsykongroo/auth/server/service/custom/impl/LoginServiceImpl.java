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

package com.elpsykongroo.auth.server.service.custom.impl;

import com.elpsykongroo.auth.server.entity.user.Authenticator;
import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.security.provider.WebAuthnAuthenticationToken;
import com.elpsykongroo.auth.server.service.custom.AuthenticatorService;
import com.elpsykongroo.auth.server.service.custom.LoginService;
import com.elpsykongroo.auth.server.service.custom.UserService;
import com.elpsykongroo.auth.server.utils.Random;
import com.elpsykongroo.services.redis.client.RedisService;
import com.elpsykongroo.services.redis.client.dto.KV;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.AuthenticatorAttestationResponse;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.ClientRegistrationExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {
    @Autowired
    private UserService userService;

    @Autowired
    private ServletContext servletContext;

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    private SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    @Autowired
    private RelyingParty relyingParty;

    @Autowired
    private AuthenticatorService authenticatorService;

    @Autowired
    private RedisService redisService;

    @Override
    public String login(String username, HttpServletRequest servletRequest) {
        DeferredSecurityContext securityContext = securityContextRepository.loadDeferredContext(servletRequest);
        if (securityContext.get().getAuthentication() != null) {
            log.debug("already login");
            if (username.equals(securityContext.get().getAuthentication().getName())) {
                return "200";
            } else {
                return "202";
            }
        }
        AssertionRequest request = relyingParty.startAssertion(StartAssertionOptions.builder()
                .username(username)
                .userVerification(UserVerificationRequirement.DISCOURAGED)
                .build());

        try {
            servletContext.setAttribute(username, request);
            return request.toCredentialsGetJson();
        } catch (JsonProcessingException e) {
            return "502";
        }
    }

    @Override
    public String handleLogin(String credential, String username, HttpServletRequest request, HttpServletResponse response) {
        try {
            User user = userService.loadUserByUsername(username);
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs>
                    pkc = PublicKeyCredential.parseAssertionResponseJson(credential);
            AssertionRequest assertionRequest = (AssertionRequest) servletContext.getAttribute(username);
            servletContext.removeAttribute(username);
            log.debug("remove pkc success");
            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(pkc)
                    .build());
            if (result.isSuccess() && !user.isLocked()) {
                Authenticator authenticator = authenticatorService.findByCredentialId(pkc.getId()).get();
                authenticatorService.updateCount(authenticator);
                log.debug("login success");
                SecurityContext context = securityContextHolderStrategy.createEmptyContext();
                Authentication authentication =
                        WebAuthnAuthenticationToken.authenticated(result.getUsername(), result.getCredential(), user.getAuthorities());
                context.setAuthentication(authentication);
                securityContextHolderStrategy.setContext(context);
                securityContextRepository.saveContext(context, request, response);
                log.debug("set SecurityContext success");
                return "200";
            } else {
                return "401";
            }
        } catch (IOException e) {
            return "400";
        } catch (AssertionFailedException e) {
            log.error("saveUser error:{}", e.getMessage());
            return "500";
        }
    }

    @Override
    public String register(String username, String display) {
        removeInvalidUser(username);
        User existingUser = userService.loadUserByUsername(username);
        if (existingUser == null) {
            UserIdentity userIdentity = UserIdentity.builder()
                    .name(username)
                    .displayName(display)
                    .id(new ByteArray(Random.generateRandomByte(32)))
                    .build();
            User saveUser = new User(userIdentity);
            saveUser.setCreateTime(Instant.now());
            saveUser.setUpdateTime(Instant.now());
            userService.add(saveUser);
            String response = registerAuth(saveUser);
            return response;
        } else {
            return "409";
        }
    }

    @Override
    public String addAuthenticator(String username) {
        User user = userService.loadUserByUsername(username);
        String response = registerAuth(user);
        return response;
    }

    private String registerAuth(User user) {
        User existingUser = userService.findByHandle(user.getHandle());
        if (existingUser != null) {
            UserIdentity userIdentity = user.toUserIdentity();
            StartRegistrationOptions registrationOptions = StartRegistrationOptions.builder()
                    .user(userIdentity)
                    .build();
            PublicKeyCredentialCreationOptions registration = relyingParty.startRegistration(registrationOptions);
            servletContext.setAttribute(user.getUsername(), registration);
            try {
                return registration.toCredentialsCreateJson();
            } catch (JsonProcessingException e) {
                return "500";
            }
        } else {
            return "409";
        }
    }

    @Override
    public String saveAuth(String credential, String username, String credname) {
        User user = userService.loadUserByUsername(username);
        PublicKeyCredentialCreationOptions requestOptions =
                (PublicKeyCredentialCreationOptions) servletContext.getAttribute(user.getUsername());
        servletContext.removeAttribute(user.getUsername());
        log.debug("remove requestOptions success");
        try {
            if (credential != null) {
                PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                        PublicKeyCredential.parseRegistrationResponseJson(credential);
                FinishRegistrationOptions options = FinishRegistrationOptions.builder()
                        .request(requestOptions)
                        .response(pkc)
                        .build();
                RegistrationResult result = relyingParty.finishRegistration(options);
                Authenticator savedAuth = new Authenticator(result, pkc.getResponse(), user, credname);
                authenticatorService.add(savedAuth);
                log.debug("save authenticator success");
                userService.updateUserInfoEmail(username + "@tmp.com", username, null, false);
                return "200";
            } else {
                removeInvalidUser(username);
                return "500";
            }
        } catch (RegistrationFailedException e) {
            log.error("finishRegistration error:{}", e.getMessage());
            return "502";
        } catch (IOException e) {
            return "400";
        }
    }

    private void removeInvalidUser(String username) {
        try {
            User user = userService.loadUserByUsername(username);
            if (user != null) {
                if (user.getHandle().isEmpty()) {
                    log.debug("remove invalid user with no handle");
                    userService.deleteByUsername(username);
                    authenticatorService.deleteByName(username);
                }
                Optional<Authenticator> authenticator = authenticatorService.findByName(username);
                if (authenticator.isPresent()) {
                    if (authenticator.get().getCredentialId().isEmpty()) {
                        log.debug("remove invalid user with no cred");
                        authenticatorService.deleteByName(username);
                        userService.deleteByUsername(username);
                    }
                } else {
                    log.debug("remove invalid user with no auth");
                    userService.deleteByUsername(username);
                }
            }
        } catch (Exception e) {
            log.error("remove invalid user error:{}", e.getMessage());
        }
    }

    public String tmpLogin(String text, HttpServletRequest request, HttpServletResponse response) {
        try {
            String[] texts = text.split("\\.");
            String codeVerifier = texts[0];
            String username = texts[1];
            String encodedVerifier = verifyChallenge(codeVerifier);
            String tmp = redisService.get("TmpCert_" + username);
            if (tmp.equals(encodedVerifier)) {
                SecurityContext context = securityContextHolderStrategy.createEmptyContext();
                Authentication authentication =
                        WebAuthnAuthenticationToken.authenticated(username, null, null);
                context.setAuthentication(authentication);
                securityContextHolderStrategy.setContext(context);
                securityContextRepository.saveContext(context, request, response);
                log.debug("set tmp SecurityContext");
                KV kv = new KV("TmpCert_" + username, "");
                redisService.set(kv);
                return "redirect:https://elpsykongroo.com";
            }
        } catch (Exception e) {
            log.error("set tmp SecurityContext error:{}", e.getMessage());
        }
        return "redirect:https://elpsykongroo.com/error";
    }

    public static String verifyChallenge(String codeVerifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            String encodedVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
            return encodedVerifier;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
