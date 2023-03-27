package com.elpsykongroo.auth.server.service.custom;

import com.elpsykongroo.auth.server.entity.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public interface UserService extends UserDetailsService {
    User loadUserByUsername(String username) throws UsernameNotFoundException;

    String login(String username, HttpServletRequest request);

    String handlelogin(String credential, String username, HttpServletRequest request, HttpServletResponse response);

    String saveUser(String credential, String username, String credname);

    String register(String username, String display);

}


