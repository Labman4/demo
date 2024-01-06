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

import com.elpsykongroo.auth.entity.user.Authority;
import com.elpsykongroo.auth.entity.user.Group;
import com.elpsykongroo.auth.entity.user.User;
import com.elpsykongroo.auth.repository.user.AuthorityRepository;
import com.elpsykongroo.auth.service.custom.AuthorityService;
import com.elpsykongroo.auth.service.custom.GroupService;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthorityServiceImpl implements AuthorityService {

    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private GroupService groupService;

    @Override
    public String addAuthority(String authority) {
        if (StringUtils.isNotBlank(authority)) {
            Optional<Authority> auth = authorityRepository.findByAuthority(authority);
            if (!auth.isPresent()) {
                return authorityRepository.save(new Authority(authority)).getAuthority();
            }
        }
        return "";
    }
    @Override
    public int deleteAuthority(String authority) {
        return authorityRepository.deleteByAuthority(authority);
    }

    @Override
    public List<Authority> authorityList() {
        return authorityRepository.findAll();
    }

    @Override
    public List<Authority> userAuthority(String id) {
        List<Group> groups = groupService.userGroup(id);
        List<Authority> authorities = authorityRepository.findByUsers_Id(id);
        for (Group group : groups) {
            authorities.addAll(group.getAuthorities());
        }
        return authorities.stream().distinct().collect(Collectors.toList());
    }

    @Transactional
    @Override
    public int updateGroupAuthority(String names, String groupId) {
        if (StringUtils.isNotBlank(names)) {
            List<Authority> authorityList = Arrays.stream(names.split(",")).map(name -> {
                Optional<Authority> authority = authorityRepository.findByAuthority(name);
                if (authority.isPresent()) {
                    return authority.get();
                } else {
                    return null;
                }
            }).collect(Collectors.toList());
            String[] ids = groupId.split(",");
            return Arrays.stream(ids).map(id -> {
                Group group = entityManager.find(Group.class, id);
                return authorityList.stream().map(authority -> {
                    Authority auth = entityManager.find(Authority.class, authority.getId());
                    if(!group.getAuthorities().contains(auth)) {
                        group.getAuthorities().add(auth);
                        entityManager.persist(group);
                    } else {
                        group.getAuthorities().remove(auth);
                        entityManager.persist(group);
                    }
                    return group;
                }).collect(Collectors.toList());
            }).collect(Collectors.toList()).size();
        } else {
            return 0;
        }
    }

    @Transactional
    @Override
    public int updateUserAuthority(String names, String userId) {
        if (StringUtils.isNotBlank(names)) {
            List<Authority> authorityList = Arrays.stream(names.split(",")).map(name -> {
                Optional<Authority> authority = authorityRepository.findByAuthority(name);
                if (authority.isPresent()) {
                    return authority.get();
                } else {
                    return null;
                }
            }).collect(Collectors.toList());
            String[] userIds = userId.split(",");
            return Arrays.stream(userIds).map(id -> {
                User user = entityManager.find(User.class, id);
                return authorityList.stream().map(authority -> {
                    Authority auth = entityManager.find(Authority.class, authority.getId());
                    if(!user.getAuthorities().contains(auth)) {
                        user.getAuthorities().add(auth);
                        entityManager.persist(user);
                    } else {
                        user.getAuthorities().remove(auth);
                        entityManager.persist(user);
                    }
                    return user;
                }).collect(Collectors.toList());
            }).collect(Collectors.toList()).size();
        } else {
            return 0;
        }
    }

    @Override
    public List<Authority> findByGroup(String name) {
        return authorityRepository.findByGroups_GroupName(name);
    }
}
