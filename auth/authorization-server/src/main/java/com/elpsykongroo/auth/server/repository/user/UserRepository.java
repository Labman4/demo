package com.elpsykongroo.auth.server.repository.user;


import com.elpsykongroo.auth.server.entity.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {

    User findByUsername(String username);
}
