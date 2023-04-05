package com.elpsykongroo.auth.server.repository.user;

import com.elpsykongroo.auth.server.entity.user.Authenticator;
import com.elpsykongroo.auth.server.entity.user.User;
import com.yubico.webauthn.data.ByteArray;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuthenticatorRepository extends JpaRepository<Authenticator, String> {
    @Transactional
    @Modifying
    @Query("update Authenticator a set a.count = ?1 where a.user = ?2")
    int updateCountByUser(Long count, User user);

    @Transactional
    long deleteByName(String name);

    Optional<Authenticator> findByName(String name);

    Optional<Authenticator> findByCredentialId(ByteArray credentialId);

    List<Authenticator> findAllByUser (User user);

    List<Authenticator> findAllByCredentialId(ByteArray credentialId);

}
