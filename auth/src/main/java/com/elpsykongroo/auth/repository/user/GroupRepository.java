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

package com.elpsykongroo.auth.repository.user;

import com.elpsykongroo.auth.entity.user.Group;
import jakarta.transaction.Transactional;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public interface GroupRepository extends CrudRepository<Group, String> {
    List<Group> findByAuthorities_Authority(String authority);

    Optional<Group> findByGroupName(String groupName);

    List<Group> findByUsers_Id(String id);

    @Transactional
    int deleteByGroupName(String groupName);

    @Override
    List<Group> findAll();

}
