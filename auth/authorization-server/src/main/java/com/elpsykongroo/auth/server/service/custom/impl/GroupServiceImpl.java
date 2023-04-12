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

import com.elpsykongroo.auth.server.entity.user.Group;
import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.repository.user.GroupRepository;
import com.elpsykongroo.auth.server.service.custom.GroupService;
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
public class GroupServiceImpl implements GroupService {
    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private EntityManager entityManager;

    @Override
    public void addGroup(String groupName) {
        if (StringUtils.isNotBlank(groupName)) {
            Optional<Group> group = groupRepository.findByGroupName(groupName);
            if (!group.isPresent()) {
                groupRepository.save(new Group(groupName));
            }
        }
    }
    @Override
    public int deleteGroup(String groupName) {
        return groupRepository.deleteByGroupName(groupName);
    }

    @Override
    public List<Group> groupList() {
        return groupRepository.findAll();
    }

    @Override
    public List<Group> userGroup(String id) {
        return groupRepository.findByUsers_Id(id);
    }

    @Override
    public List<Group> findByAuthority(String name) {
        return groupRepository.findByAuthorities_Authority(name);
    }

    @Transactional
    @Override
    public int updateUserGroup(String names, String userId) {
        if (StringUtils.isNotBlank(names)) {
            List<Group> groupList = Arrays.stream(names.split(",")).map(name -> {
                Optional<Group> group = groupRepository.findByGroupName(name);
                if (group.isPresent()) {
                    return group.get();
                } else {
                    return null;
                }
            }).collect(Collectors.toList());
            String[] userIds = userId.split(",");
            return Arrays.stream(userIds).map(id -> {
                User user = entityManager.find(User.class, id);
                return groupList.stream().map(group -> {
                    Group g = entityManager.find(Group.class, group.getId());
                    if(!user.getGroups().contains(g)) {
                        user.getGroups().add(g);
                        entityManager.persist(user);
                    } else {
                        user.getGroups().remove(g);
                        entityManager.persist(user);
                    }
                    return user;
                }).collect(Collectors.toList());
            }).collect(Collectors.toList()).size();
        } else {
            return 0;
        }
    }
}
