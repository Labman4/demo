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

package com.elpsykongroo.auth.service.custom.impl;

import com.elpsykongroo.auth.entity.user.Authenticator;
import com.elpsykongroo.auth.entity.user.Authority;
import com.elpsykongroo.auth.entity.user.User;
import com.elpsykongroo.auth.security.provider.WebAuthnAuthenticationToken;
import com.elpsykongroo.auth.service.custom.AuthenticatorService;
import com.elpsykongroo.auth.service.custom.AuthorityService;
import com.elpsykongroo.auth.service.custom.AuthorizationService;
import com.elpsykongroo.auth.service.custom.EmailService;
import com.elpsykongroo.auth.service.custom.LoginService;
import com.elpsykongroo.auth.service.custom.UserService;
import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.base.domain.message.Message;
import com.elpsykongroo.base.service.MessageService;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.utils.BytesUtils;
import com.elpsykongroo.base.utils.JsonUtils;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
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

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
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

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private OpaqueTokenIntrospector tokenIntrospector;

    @Autowired
    private ServiceConfig serviceConfig;

    @Autowired
    private MessageService messageService;

    @Override
    public String login(String username, HttpServletRequest servletRequest, HttpServletResponse response) {
//        DeferredSecurityContext securityContext = securityContextRepository.loadDeferredContext(servletRequest);
//        if (securityContext.get().getAuthentication() != null) {
//            if (log.isDebugEnabled()) {
//                log.debug("already login");
//            }
//            if (username.equals(securityContext.get().getAuthentication().getName())) {
//                return "200";
//            } else {
//                return "202";
//            }
//        } else {
//            SecurityContext context = securityContextHolderStrategy.createEmptyContext();
//            Authentication authentication =
//                    WebAuthnAuthenticationToken.authenticated(
//                            username,
//                            null,
//                            null);
//            context.setAuthentication(authentication);
//            securityContextHolderStrategy.setContext(context);
//            securityContextRepository.saveContext(context, servletRequest, response);
//            return "200";
//        }

        try {
            DeferredSecurityContext securityContext = securityContextRepository.loadDeferredContext(servletRequest);
            if (securityContext.get().getAuthentication() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("already login");
                }
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
                    servletContext.setAttribute(username, request.toJson());
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
            AssertionRequest assertionRequest = JsonUtils.toObject(servletContext.getAttribute(username).toString(), AssertionRequest.class);
            servletContext.removeAttribute(username);
            if (log.isDebugEnabled()) {
                log.debug("remove pkc success");
            }
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
                /**
                 * user byteArray cant serializable and do not need add authorities with authentication when use OpaqueToken
                 */
                Authentication authentication =
                        WebAuthnAuthenticationToken.authenticated(
                                username,
                                null,
                                null);
                context.setAuthentication(authentication);
                securityContextHolderStrategy.setContext(context);
                securityContextRepository.saveContext(context, request, response);
                if (log.isDebugEnabled()) {
                    log.debug("set SecurityContext success");
                }
                SavedRequest savedRequest = requestCache.getRequest(request, response);
                if (savedRequest != null
                        && savedRequest.getRedirectUrl() != null
                        && savedRequest.getRedirectUrl().contains("oauth2/authorize?client_id")) {
                    if (log.isDebugEnabled()) {
                        log.debug("get saved authorize url");
                    }
                    return savedRequest.getRedirectUrl();
                }
                return "200";
            } else {
                return "401";
            }
        } catch (IOException e) {
            return "400";
        } catch (AssertionFailedException e) {
            if (log.isErrorEnabled()) {
                log.error("login error:{}", e);
            }
            return "500";
        }
    }

    @Override
    public String register(String username, String display) {
        if ("admin".equals(username)) {
            return "409";
        }
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
            if (log.isErrorEnabled()) {
                log.error("register with error:{}", e.getMessage());
            }
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
                .id(new ByteArray(BytesUtils.generateRandomByte(32)))
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
        if (log.isDebugEnabled()) {
            log.debug("remove requestOptions success");
        }
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
                if (log.isDebugEnabled()) {
                    log.debug("save authenticator success");
                }
                if(user.getUserInfo() == null || user.getUserInfo().isEmpty()) {
                    userService.updateUserInfoEmail(username + "@tmp.com", username, null, false);
                }
                return "200";
            } else {
                removeInvalidUser(username, "");
                return "500";
            }
        } catch (RegistrationFailedException e) {
            if (log.isErrorEnabled()) {
                log.error("finishRegistration error:{}", e.getMessage());
            }
            return "502";
        } catch (IOException e) {
            return "400";
        }
    }

    private void removeInvalidUser(String username, String id) {
        int result = 0;
        if (userService.validUser(username, id)) {
            if (log.isDebugEnabled()) {
                log.debug("remove invalid user with no handle");
            }
            result += userService.deleteByUsername(username);
            authenticatorService.deleteByName(username);
        }
        List<Authenticator> authenticators = authenticatorService.findByUser(username);
        int inValidCount = 0;
        for (Authenticator authenticator : authenticators) {
            if (authenticator.getCredentialId().isEmpty()) {
                if (log.isDebugEnabled()) {
                    log.debug("remove invalid user with no cred");
                }
                authenticatorService.deleteById(authenticator.getId());
                inValidCount++;
            }
        }
        if (inValidCount >= authenticators.size()) {
            if (log.isDebugEnabled()) {
                log.debug("remove user with no valid cred");
            }
            result += userService.deleteByUsername(username);
        }
        if (log.isDebugEnabled()) {
            log.debug("remove user result:{}", result);
        }
    }

    @Override
    public String tmpLogin(String text, HttpServletRequest request, HttpServletResponse response) {
        try {
            String[] texts = text.split("\\.");
            String codeVerifier = texts[0];
            String username = texts[1];
            String encodedVerifier = PkceUtils.verifyChallenge(codeVerifier);
            String tmp = redisService.get("TmpCert_" + username);
            if (StringUtils.isNotBlank(tmp) && tmp.equals(encodedVerifier)) {
                SecurityContext context = securityContextHolderStrategy.createEmptyContext();
                Authentication authentication =
                        WebAuthnAuthenticationToken.authenticated(username, null, null);
                context.setAuthentication(authentication);
                securityContextHolderStrategy.setContext(context);
                securityContextRepository.saveContext(context, request, response);
                if (log.isDebugEnabled()) {
                    log.debug("set tmp SecurityContext");
                }
                redisService.set("TmpCert_" + username, "", "1");
                return "redirect:" + serviceConfig.getUrl().getLoginPage() + "?username=" + username;
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("set tmp SecurityContext error:{}", e.getMessage());
            }
        }
        return "redirect:" + serviceConfig.getUrl().getLoginPage() + "/error";
    }

    @Override
    public String loginWithToken(String token, String idToken, HttpServletRequest request, HttpServletResponse response) {
        try {
            OAuth2AuthenticatedPrincipal result = tokenIntrospector.introspect(token);
            if (log.isDebugEnabled()) {
                log.debug("introspect:{}", result.getAttribute("active").toString());
            }
            if ((boolean) result.getAttribute("active")) {
                Jwt id = jwtDecoder.decode(idToken);
                if (log.isDebugEnabled()) {
                    log.debug("token user:{}", id.getSubject());
                }
                SecurityContext context = securityContextHolderStrategy.createEmptyContext();
                Authentication authentication =
                        WebAuthnAuthenticationToken.authenticated(
                                id.getSubject(),
                                null,
                                null);
                context.setAuthentication(authentication);
                securityContextHolderStrategy.setContext(context);
                securityContextRepository.saveContext(context, request, response);
                return "200";
            } else {
                return "401";
            }
        } catch (JwtException e) {
            return "500";
        } catch (UsernameNotFoundException e) {
            return "404";
        }
    }

    @Override
    public String setToken(String text) {
        String encodedVerifier = PkceUtils.verifyChallenge(text);
        String challenge = redisService.get("PKCE-" + text);
        if (StringUtils.isNotBlank(challenge) && challenge.equals(encodedVerifier)) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String token = authorizationService.getToken(authentication.getPrincipal().toString());
            if (StringUtils.isNotEmpty(token)) {
                Message message = new Message();
                message.setKey(text);
                message.setValue(token);
                messageService.setMessage(message);
                return "200";
            } else {
                return "404";
            }
        }
        return "400";
    }

    private User initAdminUser() {
        if (log.isDebugEnabled()) {
            log.debug("init admin");
        }
        User user = saveUser("admin", "admin");
        if (StringUtils.isNotBlank(adminEmail)) {
            userService.updateUserInfoEmail(adminEmail, "admin", null, true);
        }
        return user;
    }

    private void initAdminAuth(User user) {
        String[] init = initAdminAuth.split(",");
        List<Authority> existAuth = userService.userAuthority(user.getUsername());
        for (String auth: init ) {
            boolean exist = false;
            for (Authority authority: existAuth) {
                if(authority.getAuthority().equals(auth)){
                    exist = true;
                }
            }
            if (!exist) {
                if (log.isDebugEnabled()) {
                    log.debug("init auth with:{}", auth);
                }
                authorityService.updateUserAuthority(auth, user.getId());
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
