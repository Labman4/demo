package com.elpsykongroo.auth.server.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;

import com.elpsykongroo.auth.server.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    User findByUsername(String username);
}
