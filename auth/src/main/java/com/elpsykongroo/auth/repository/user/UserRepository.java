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

import com.elpsykongroo.auth.entity.user.User;
import com.yubico.webauthn.data.ByteArray;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public interface UserRepository extends JpaRepository<User, String> {
    @Transactional
    @Modifying
    @Query("delete from User u where u.username = ?1")
    int deleteByUsername(String username);

    @Query("select u from User u where u.username = ?1")
    List<User> findAllByUsername(String username);

    boolean existsByHandleNullOrAuthenticatorsEmptyAndUsername(String username);

    boolean existsByHandleNullOrAuthenticatorsEmptyAndId(String id);

    long countByUsername(String username);

    @Transactional
    @Modifying
    @Query("update User u set u.email = ?1 where u.username = ?2")
    int updateEmailByUsername(String email, String username);

    @Transactional
    @Modifying
    @Query("""
            update User u set u.nickName = ?1, u.locked = ?2, u.password = ?3, u.updateTime = ?4
            where u.username = ?5""")
    int updateUser(String nickName, boolean locked, String password, Instant time, String username);

    @Transactional
    @Modifying
    @Query("update User u set u.userInfo = ?1 where u.username = ?2")
    int updateUserInfoByUsername(Map<String, Object> userInfo, String username);

    User findByUsername(String username);

    User findByHandle(ByteArray handle);

}
