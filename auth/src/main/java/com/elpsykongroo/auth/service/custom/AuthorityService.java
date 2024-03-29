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

package com.elpsykongroo.auth.service.custom;

import com.elpsykongroo.auth.entity.user.Authority;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AuthorityService {

    String addAuthority(String authority);

    int deleteAuthority(String authority);

    List<Authority> authorityList();

    List<Authority> userAuthority(String id);

    int updateGroupAuthority(String authorities, String id);

    int updateUserAuthority(String authorities, String id);

    List<Authority> findByGroup(String name);

}
