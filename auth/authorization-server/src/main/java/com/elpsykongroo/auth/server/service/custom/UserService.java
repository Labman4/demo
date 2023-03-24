package com.elpsykongroo.auth.server.service.custom;

import com.elpsykongroo.auth.server.entity.user.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public interface UserService extends UserDetailsService {
    User loadUserByUsername(String username) throws UsernameNotFoundException;
}
