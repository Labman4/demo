package com.elpsykongroo.auth.server.web;

import com.elpsykongroo.auth.server.entity.user.User;
import com.elpsykongroo.auth.server.entity.user.UserInfo;
import com.elpsykongroo.auth.server.service.custom.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin("*")
@RestController
@RequestMapping("/auth/user")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public String userList(
            @RequestParam("pageNumber") String pageNumber,
            @RequestParam("pageSize") String pageSize,
            @RequestParam("order") String order
            ) {
        return userService.list(pageNumber, pageSize, order).toString();
    }

    @GetMapping("/info")
    public String loadUserInfo(
            @RequestParam String username
    ) {
        return userService.loadUserInfo(username);
    }

    @PatchMapping("/patch")
    public String updateUser(
            @RequestBody User user
    ) {
        if (userService.updateUser(user) > 0) {
            return "done";
        }
        return "0";
    }

    @PatchMapping("/info/patch")
    public String updateUserInfo(
            @RequestBody UserInfo userInfo
    ) {
        if (userService.updateUserInfo(userInfo) > 0) {
            return "done";
        }
        return "0";
    }
}
