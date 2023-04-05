package com.elpsykongroo.auth.server.service.custom;

import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.entity.user.UserInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface UserService extends UserDetailsService {
    User loadUserByUsername(String username) throws UsernameNotFoundException;

    String login(String username, HttpServletRequest request);

    String handlelogin(String credential, String username, HttpServletRequest request, HttpServletResponse response);

    String saveAuth(String credential, String username, String credname);

    String register(String username, String display);

    int updateUserInfo(UserInfo userinfo);

    int updateUser(User user);

    OidcUserInfo loadUser(String username);

    String loadUserInfo(String username);

    List<User> list(String pageNumber, String pageSize, String order);

}


