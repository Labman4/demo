package com.elpsykongroo.auth.server.service.custom.impl;

import com.elpsykongroo.auth.server.entity.user.Authenticator;
import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.repository.user.UserRepository;
import com.elpsykongroo.auth.server.service.custom.UserService;
import com.elpsykongroo.auth.server.service.webauthn.RegistrationService;
import com.elpsykongroo.auth.server.service.webauthn.WebAuthnAuthenticationToken;
import com.elpsykongroo.auth.server.utils.Random;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ServletContext servletContext;

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    private SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Autowired
    private RelyingParty relyingParty;

    @Autowired
    private RegistrationService service;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username);
    }

    @Override
    public String login(String username, HttpServletRequest servletRequest) {
        DeferredSecurityContext securityContext = securityContextRepository.loadDeferredContext(servletRequest);
        if (securityContext.get().getAuthentication() != null) {
            log.info("already login");
            return "200";
        }
        AssertionRequest request = relyingParty.startAssertion(StartAssertionOptions.builder()
                .username(username)
                .build());
        try {
            servletContext.setAttribute(username, request);
            return request.toCredentialsGetJson();
        } catch (JsonProcessingException e) {
            return "502";
        }
    }

    @Override
    public String handlelogin(String credential, String username, HttpServletRequest request, HttpServletResponse response) {
        try {
            User user = service.getUserRepo().findByUsername(username);
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
                log.info("login success");
                SecurityContext context = securityContextHolderStrategy.createEmptyContext();
                Authentication authentication =
                        WebAuthnAuthenticationToken.authenticated(result.getUsername(), result.getCredential(), user.getAuthorities());
                context.setAuthentication(authentication);
                securityContextHolderStrategy.setContext(context);
                securityContextRepository.saveContext(context, request, response);
                log.info("set SecurityContext success");
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
    public String saveUser(String credential, String username, String credname) {
        User user = service.getUserRepo().findByUsername(username);
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
                service.getAuthRepository().save(savedAuth);
                log.info("save authenticator success");
                return "200";
            } else {
                return "500";
            }
        } catch (RegistrationFailedException e) {
            log.error("finishRegisration error:{}", e.getMessage());
            return "502";
        } catch (IOException e) {
            return "400";
        }
    }

    @Override
    public String register(String username, String display) {
        User existingUser = service.getUserRepo().findByUsername(username);
        if (existingUser == null) {
            UserIdentity userIdentity = UserIdentity.builder()
                    .name(username)
                    .displayName(display)
                    .id(Random.generateRandom(32))
                    .build();
            User saveUser = new User(userIdentity);
            service.getUserRepo().save(saveUser);
            String response = registerauth(saveUser);
            return response;
        } else {
            return "409";
        }
    }

    private String registerauth(User user) {
        User existingUser = service.getUserRepo().findByHandle(user.getHandle());
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

}
