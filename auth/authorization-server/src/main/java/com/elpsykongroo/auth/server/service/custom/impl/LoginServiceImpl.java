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
import com.elpsykongroo.auth.server.entity.user.Authority;
import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.security.provider.WebAuthnAuthenticationToken;
import com.elpsykongroo.auth.server.service.custom.AuthenticatorService;
import com.elpsykongroo.auth.server.service.custom.AuthorityService;
import com.elpsykongroo.auth.server.service.custom.AuthorizationService;
import com.elpsykongroo.auth.server.service.custom.EmailService;
import com.elpsykongroo.auth.server.service.custom.LoginService;
import com.elpsykongroo.auth.server.service.custom.UserService;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.utils.PkceUtils;
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
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    @Value("${service.adminEmail}")
    private String adminEmail;

    @Value("${service.initAdminAuth}")
    private String initAdminAuth;

    @Autowired
    private UserService userService;

    @Autowired
    private ServletContext servletContext;

    @Autowired
    private HttpSessionRequestCache requestCache;

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    @Autowired
    private RelyingParty relyingParty;

    @Autowired
    private AuthenticatorService authenticatorService;

    @Autowired
    private RedisService redisService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public String login(String username, HttpServletRequest servletRequest) {
        try {
            DeferredSecurityContext securityContext = securityContextRepository.loadDeferredContext(servletRequest);
            if (securityContext.get().getAuthentication() != null) {
                log.debug("already login");
                if (username.equals(securityContext.get().getAuthentication().getName())) {
                    return "200";
                } else {
                    return "202";
                }
            }
            User user = userService.loadUserByUsername(username);
            if (user != null) {
                if (user.isLocked()) {
                    return "401";
                }
                if (existAuth(user)) {
                    AssertionRequest request = relyingParty.startAssertion(StartAssertionOptions.builder()
                            .username(username)
                            .build());
                    servletContext.setAttribute(username, request);
                    return request.toCredentialsGetJson();
                } else {
                    return "400";
                }
            } else {
                if ("admin".equals(username)){
                    initAdminAuth(initAdminUser());
                    emailService.sendTmpLoginCert("admin");
                    return "400";
                }
                return "404";
            }
        } catch (JsonProcessingException e) {
            return  "500";
        }
    }

    @Override
    public String handleLogin(String credential, String username, HttpServletRequest request, HttpServletResponse response) {
        try {
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs>
                    pkc = PublicKeyCredential.parseAssertionResponseJson(credential);
            AssertionRequest assertionRequest = (AssertionRequest) servletContext.getAttribute(username);
            servletContext.removeAttribute(username);
            log.debug("remove pkc success");
            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(pkc)
                    .build());
            if (result.isSuccess()) {
                /**
                 * multi device sign count will always as 0, dont update it
                 */
//                authenticatorService.updateCount(
//                        result.getSignatureCount(),
//                        result.getCredential().getCredentialId());
                log.debug("login success");
                SecurityContext context = securityContextHolderStrategy.createEmptyContext();
                Authentication authentication =
                        WebAuthnAuthenticationToken.authenticated(
                                result.getUsername(),
                                result.getCredential(),
                                userService.loadUserByUsername(username).getAuthorities());
                context.setAuthentication(authentication);
                securityContextHolderStrategy.setContext(context);
                securityContextRepository.saveContext(context, request, response);
                log.debug("set SecurityContext success");
                SavedRequest savedRequest = requestCache.getRequest(request, response);
                if (savedRequest != null
                        && savedRequest.getRedirectUrl() != null
                        && savedRequest.getRedirectUrl().contains("oauth2/authorize?client_id")) {
                    log.debug("get saved authorize url");
                    return savedRequest.getRedirectUrl();
                }
                return "200";
            } else {
                return "401";
            }
        } catch (IOException e) {
            return "400";
        } catch (AssertionFailedException e) {
            log.error("login error:{}", e);
            return "500";
        }
    }

    @Override
    public String register(String username, String display) {
        User saveUser = null;
        try {
            removeInvalid(username);
            Long count = userService.countUser(username);
            if (count == 0) {
                saveUser = saveUser(username, display);
            }  else {
                return "409";
            }
        } catch (Exception e) {
            log.error("register with error:{}", e.getMessage());
        }
        return registerAuth(saveUser);
    }

    private void removeInvalid(String username) {
        Long count = userService.countUser(username);
        if (count <= 1) {
            removeInvalidUser(username, "");
        }  else {
            List<User> users = userService.findByUsername(username);
            users.stream().forEach(user -> removeInvalidUser(username, user.getId()));
        }
    }

    private User saveUser(String username, String display) {
        UserIdentity userIdentity = UserIdentity.builder()
                .name(username)
                .displayName(display)
                .id(new ByteArray(PkceUtils.generateRandomByte(32)))
                .build();
        User saveUser = new User(userIdentity);
        saveUser.setCreateTime(Instant.now());
        saveUser.setUpdateTime(Instant.now());
        userService.add(saveUser);
        return saveUser;
    }

    @Override
    public String addAuthenticator(String username) {
        User user = userService.loadUserByUsername(username);
        return registerAuth(user);
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
                if(user.getUserInfo() == null || user.getUserInfo().isEmpty()) {
                    userService.updateUserInfoEmail(username + "@tmp.com", username, null, false);
                }
                return "200";
            } else {
                removeInvalidUser(username, "");
                return "500";
            }
        } catch (RegistrationFailedException e) {
            log.error("finishRegistration error:{}", e.getMessage());
            return "502";
        } catch (IOException e) {
            return "400";
        }
    }

    private void removeInvalidUser(String username, String id) {
        int result = 0;
        if (userService.ValidUser(username, id)) {
            log.debug("remove invalid user with no handle");
            result += userService.deleteByUsername(username);
            authenticatorService.deleteByName(username);
        }
        List<Authenticator> authenticators = authenticatorService.findByUser(username);
        int inValidCount = 0;
        for (Authenticator authenticator : authenticators) {
            if (authenticator.getCredentialId().isEmpty()) {
                log.debug("remove invalid user with no cred");
                authenticatorService.deleteById(authenticator.getId());
                inValidCount++;
            }
        }
        if (inValidCount >= authenticators.size()) {
            log.debug("remove user with no valid cred");
            result += userService.deleteByUsername(username);
        }
        log.debug("remove user result:{}", result);
    }

    public String tmpLogin(String text, HttpServletRequest request, HttpServletResponse response) {
        try {
            String[] texts = text.split("\\.");
            String codeVerifier = texts[0];
            String username = texts[1];
            String encodedVerifier = verifyChallenge(codeVerifier);
            String tmp = redisService.get("TmpCert_" + username);
            if (StringUtils.isNotBlank(tmp) && tmp.equals(encodedVerifier)) {
                SecurityContext context = securityContextHolderStrategy.createEmptyContext();
                Authentication authentication =
                        WebAuthnAuthenticationToken.authenticated(username, null, null);
                context.setAuthentication(authentication);
                securityContextHolderStrategy.setContext(context);
                securityContextRepository.saveContext(context, request, response);
                log.debug("set tmp SecurityContext");
                redisService.set("TmpCert_" + username, "", "");
                return "redirect:https://elpsykongroo.com?username=" + username;
            }
        } catch (Exception e) {
            log.error("set tmp SecurityContext error:{}", e.getMessage());
        }
        return "redirect:https://elpsykongroo.com/error";
    }

    @Override
    public String qrcode() {
        String codeVeifier = PkceUtils.generateVerifier();
        Instant instant = Instant.now();
        redisService.set("QR_CODE-" + instant, PkceUtils.generateChallenge(codeVeifier), "5");
        return codeVeifier + "*" + instant;
    }

    @Override
    public String checkQrcode(String text) {
        return redisService.get("QR_CODE-token-" + text);
    }

    @Override
    public String setToken(String text) {
        String[] texts= text.split("\\*");
        String codeVerifier = texts[0];
        String timestamp = texts[1];
        String encodedVerifier = verifyChallenge(codeVerifier);
        String challenge = redisService.get("QR_CODE-" + timestamp);
        if (StringUtils.isNotBlank(challenge) && challenge.equals(encodedVerifier)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String token = authorizationService.getToken(authentication.getPrincipal().toString(), timestamp);
            redisService.set("QR_CODE-token-" + codeVerifier, token, "");
            redisService.set("QR_CODE-" + timestamp , "", "1");
            return "200";
        }
        return "400";
    }

    public static String verifyChallenge(String codeVerifier) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private User initAdminUser() {
        log.debug("init admin");
        User user = saveUser("admin", "admin");
        if (StringUtils.isNotBlank(adminEmail)) {
            userService.updateUserInfoEmail(adminEmail, "admin", null, true);
        }
        return user;
    }

    private void initAdminAuth(User user) {
        String[] init = initAdminAuth.split(",");
        List<Authority> existAuth = userService.userAuthority(user.getUsername());
        for (int i = 0; i < init.length; i++) {
            boolean exist = false;
            for (Authority authority: existAuth) {
                if(authority.getAuthority().equals(init[i])){
                    exist = true;
                }
            }
            if (!exist) {
                log.debug("init auth with:{}", init[i]);
                authorityService.updateUserAuthority(init[i], user.getId());
            }
        }
    }

    private Boolean existAuth(User user) {
        if ("admin".equals(user.getUsername())) {
            initAdminAuth(user);
        }
        if (user.getAuthenticators().isEmpty()) {
            emailService.sendTmpLoginCert(user.getUsername());
            return false;
        }
        return true;
    }
}
