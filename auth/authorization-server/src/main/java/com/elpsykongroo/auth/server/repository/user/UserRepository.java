package com.elpsykongroo.auth.server.repository.user;

import com.elpsykongroo.auth.server.entity.user.User;
import com.yubico.webauthn.data.ByteArray;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;


public interface UserRepository extends JpaRepository<User, String> {
    @Transactional
    @Modifying
    @Query("update User u set u.email = ?1 where u.username = ?2")
    int updateEmailByUsername(String email, String username);

    @Transactional
    @Modifying
    @Query("""
            update User u set u.email = ?1, u.nickName = ?2, u.locked = ?3, u.username = ?4, u.password = ?5, u.updateTime = ?6
            where u.username = ?7""")
    int updateUser(String email, String nickName, boolean locked, String username, String password, Instant time, String username1);

    @Transactional
    @Modifying
    @Query("update User u set u.userInfo = ?1 where u.username = ?2")
    int updateUserInfoByUsername(Map<String, Object> userInfo, String username);

    User findByUsername(String username);

    User findByHandle(ByteArray handle);

    @Transactional
    int  deleteByUsername(String username);

}
