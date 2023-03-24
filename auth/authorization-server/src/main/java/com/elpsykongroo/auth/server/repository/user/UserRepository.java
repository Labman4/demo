package com.elpsykongroo.auth.server.repository.user;


import com.elpsykongroo.auth.server.entity.user.User;
import com.yubico.webauthn.data.ByteArray;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {

    User findByUsername(String username);

    User findByHandle(ByteArray handle);

}
