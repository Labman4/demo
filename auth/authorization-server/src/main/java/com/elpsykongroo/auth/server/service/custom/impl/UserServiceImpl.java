package com.elpsykongroo.auth.server.service.custom.impl;

import com.elpsykongroo.auth.server.entity.user.Authenticator;
import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.entity.user.UserInfo;
import com.elpsykongroo.auth.server.repository.user.AuthenticatorRepository;
import com.elpsykongroo.auth.server.repository.user.UserRepository;
import com.elpsykongroo.auth.server.service.custom.UserService;
import com.elpsykongroo.auth.server.service.webauthn.RegistrationService;
import com.elpsykongroo.auth.server.service.webauthn.WebAuthnAuthenticationToken;
import com.elpsykongroo.auth.server.utils.JsonUtils;
import com.elpsykongroo.auth.server.utils.Random;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.DeferredSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticatorRepository authRepository;

    @Autowired
    private ServletContext servletContext;

    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    private SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @Autowired
    private RelyingParty relyingParty;

    @Autowired
    private RegistrationService service;

    @Autowired
    private EntityManager entityManager;

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
            User user = userRepository.findByUsername(username);
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
    public String saveAuth(String credential, String username, String credname) {
        User user = userRepository.findByUsername(username);
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
                authRepository.save(savedAuth);
                log.info("save authenticator success");
                return "200";
            } else {
                removeInvalidUser(username);
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
        removeInvalidUser(username);
        User existingUser = userRepository.findByUsername(username);
        if (existingUser == null) {
            UserIdentity userIdentity = UserIdentity.builder()
                    .name(username)
                    .displayName(display)
                    .id(Random.generateRandom(32))
                    .build();
            User saveUser = new User(userIdentity);
            saveUser.setCreateTime(Instant.now());
            saveUser.setUpdateTime(Instant.now());
            userRepository.save(saveUser);
            String response = registerAuth(saveUser);
            return response;
        } else {
            return "409";
        }
    }

    private void removeInvalidUser(String username) {
        try {
            User user = userRepository.findByUsername(username);
            if (user != null) {
                if (user.getHandle().isEmpty()) {
                    log.info("remove invalid user with no handle");
                    userRepository.deleteByUsername(username);
                    authRepository.deleteByName(username);
                }
                Optional<Authenticator> authenticator = authRepository.findByName(username);
                if (authenticator.isPresent()) {
                    if (authenticator.get().getCredentialId().isEmpty()) {
                        log.info("remove invalid user with no cred");
                        authRepository.deleteByName(username);
                        userRepository.deleteByUsername(username);
                    }
                } else {
                    log.info("remove invalid user with no auth");
                    userRepository.deleteByUsername(username);
                }
            }
        } catch (Exception e) {
            log.error("remove invalid user error:{}", e.getMessage());
        }
    }

    @Override
    public int updateUserInfo(UserInfo info) {
        Consumer<Map<String, Object>> claims = null;
        Map<String, Object> userInfo = null;
        try {
            if (info.getClaims() != null && !info.getClaims().isEmpty()) {
                Map<String, Object> claim = objectMapper.readValue(info.getClaims(), new TypeReference<Map<String, Object>>() {});
                claims = claimMap -> {
                    claim.forEach((key, value) -> {
                        claimMap.put(key, value);
                    });
                };
            }
        } catch (JsonProcessingException e) {
            log.error("json parse error:{}", e.getMessage());
        }

        OidcUserInfo.Builder builder = OidcUserInfo.builder()
                .subject(info.getSub())
                .name(info.getName())
                .givenName(info.getGiven_name())
                .familyName(info.getFamily_name())
                .middleName(info.getMiddle_name())
                .nickname(info.getNickname())
                .preferredUsername(info.getPreferred_username())
                .profile(info.getProfile())
                .picture(info.getPicture())
                .website(info.getWebsite())
                .email(info.getEmail())
                .emailVerified(Boolean.parseBoolean(info.getEmail_verified()))
                .gender(info.getGender())
                .birthdate(info.getBirthdate())
                .zoneinfo(info.getZoneinfo())
                .locale(info.getLocale())
                .phoneNumber(info.getPhone_number())
                .phoneNumberVerified(Boolean.parseBoolean(info.getPhone_number_verified()))
                .updatedAt(Instant.now().toString());
        if (claims == null) {
            userInfo = builder.build().getClaims();
        } else {
            userInfo = builder.claims(claims).build().getClaims();
        }

        if (StringUtils.isNotBlank(info.getEmail()) && "true".equals(info.getEmail_verified())) {
            userRepository.updateEmailByUsername(info.getEmail(), info.getUsername());
        }

        return userRepository.updateUserInfoByUsername(userInfo, info.getUsername());
    }

    @Override
    public int updateUser(User user) {
        return userRepository.updateUser(
                user.getEmail(),
                user.getNickName(),
                user.isLocked(),
                user.getUsername(),
                user.getPassword(),
                Instant.now(),
                user.getUsername());
    }

    @Override
    public OidcUserInfo loadUser(String username) {
        Map<String, Object> claims = loadUserByUsername(username).getUserInfo();
        if (claims != null) {
            return new OidcUserInfo(claims);
        }
        return null;
    }

    @Override
    public String loadUserInfo(String username) {
        return JsonUtils.toJson(loadUserByUsername(username).getUserInfo());
    }


    @Override
    public List<User> list(String pageNumber, String pageSize, String order) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        if ("1".equals(order)) {
            sort = Sort.by(Sort.Direction.ASC, "createTime");
        }
        Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize), sort);
        Page<User> users = userRepository.findAll(pageable);
        return users.get().toList();
    }


    private String registerAuth(User user) {
        User existingUser = userRepository.findByHandle(user.getHandle());
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
//    private void updateCount(User user) {
////        authRepository.updateCountByUser();
//    }
}
