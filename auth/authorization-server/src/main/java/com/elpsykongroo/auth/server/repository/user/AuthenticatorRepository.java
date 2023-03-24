package com.elpsykongroo.auth.server.repository.user;

import com.elpsykongroo.auth.server.entity.user.Authenticator;
import com.elpsykongroo.auth.server.entity.user.User;
import com.yubico.webauthn.data.ByteArray;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthenticatorRepository extends JpaRepository<Authenticator, Integer> {
    Optional<Authenticator> findByCredentialId(ByteArray credentialId);
    List<Authenticator> findAllByUser (User user);
    List<Authenticator> findAllByCredentialId(ByteArray credentialId);
}
