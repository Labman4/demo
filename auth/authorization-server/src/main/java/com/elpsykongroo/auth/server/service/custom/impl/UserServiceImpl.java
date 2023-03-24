package com.elpsykongroo.auth.server.service.custom.impl;

import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.repository.user.UserRepository;
import com.elpsykongroo.auth.server.service.custom.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public User loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username);
    }
}
