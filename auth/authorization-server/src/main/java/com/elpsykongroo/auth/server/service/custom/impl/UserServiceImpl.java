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

import com.elpsykongroo.auth.server.entity.user.Authority;
import com.elpsykongroo.auth.server.entity.user.Group;
import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.entity.user.UserInfo;
import com.elpsykongroo.auth.server.repository.user.UserRepository;
import com.elpsykongroo.auth.server.service.custom.UserService;
import com.elpsykongroo.base.utils.JsonUtils;
import com.fasterxml.jackson.core.type.TypeReference;

import com.yubico.webauthn.data.ByteArray;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> findByUsername(String username) {
        return userRepository.findAllByUsername(username);
    }

    @Override
    public int updateUserInfo(UserInfo info) {
        Consumer<Map<String, Object>> claims = null;
        Map<String, Object> userInfo = null;
        if (info.getClaims() != null) {
            Map<String, Object> claim = JsonUtils.toType(info.getClaims(), new TypeReference<Map<String, Object>>() {});
            if (claim != null && !claim.isEmpty()) {
                claims = claimMap -> {
                    claim.forEach((key, value) -> {
                        claimMap.put(key, value);
                    });
                };
            }
        }

        OidcUserInfo.Builder builder = getBuilder(info);
        if (claims == null) {
            userInfo = builder.build().getClaims();
        } else {
            userInfo = builder.claims(claims).build().getClaims();
        }
        int result = userRepository.updateUserInfoByUsername(userInfo, info.getUsername());
        if (StringUtils.isNotBlank(info.getEmail()) && "true".equals(info.getEmail_verified())) {
            updateEmail(info.getEmail(), info.getUsername(), true);
        }
        return result;
    }

    @Override
    public void updateUserInfoEmail(String email, String username, Map<String, Object> userInfo, Boolean emailVerified) {
        if (userInfo == null) {
            UserInfo info = new UserInfo();
            info.setEmail(email);
            info.setEmail_verified(Boolean.toString(emailVerified));
            info.setUsername(username);
            updateUserInfo(info);
        } else {
            userInfo.put("email", email);
            userInfo.put("email_verified", emailVerified);
            userRepository.updateUserInfoByUsername(userInfo, username);
        }
    }

    @Override
    public int updateUser(User user) {
        updateEmail(user.getEmail(), user.getUsername(), false);
        return userRepository.updateUser(
                    user.getNickName(),
                    user.isLocked(),
                    user.getPassword(),
                    Instant.now(),
                    user.getUsername());
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

    @Override
    public int deleteByUsername(String username) {
        return userRepository.deleteByUsername(username);
    }

    @Override
    public User add(User user) {
        return userRepository.save(user);
    }

    @Override
    public User findByHandle(ByteArray handle) {
        return userRepository.findByHandle(handle);
    }

    @Override
    public List<Authority> userAuthority(String username) {
        User user = userRepository.findByUsername(username);
        List<Authority> authorities = new ArrayList<>();
        authorities.addAll(user.getAuthorities());
        for (Group group : user.getGroups()) {
            authorities.addAll(group.getAuthorities());
        }
        return authorities;
    }

    @Override
    public long countUser(String username) {
        return userRepository.countByUsername(username);
    }

    @Override
    public boolean ValidUser(String username, String id) {
        if (StringUtils.isNotEmpty(id)) {
           return userRepository.existsByHandleNullOrAuthenticatorsEmptyAndId(username);
        }
        return userRepository.existsByHandleNullOrAuthenticatorsEmptyAndUsername(username);
    }

    private void updateEmail(String email, String username, Boolean verify) {
        User user = loadUserByUsername(username);
        if (user != null) {
            if (!email.equals(user.getEmail())) {
                updateUserInfoEmail(email, username, user.getUserInfo(), verify);
                userRepository.updateEmailByUsername(email, username);
            }
        }
    }

    private static OidcUserInfo.Builder getBuilder(UserInfo info) {
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
        return builder;
    }
}
