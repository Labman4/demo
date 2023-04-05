package com.elpsykongroo.auth.server.service.custom;

import com.elpsykongroo.auth.server.entity.user.Authority;

import java.util.List;

public interface AuthorityService {

    void addAuthority(String authority);

    int deleteAuthority(String authority);

    List<Authority> authorityList();

    List<Authority> userAuthority(String id);

    int updateGroupAuthority(String authorities, String id);

    int updateUserAuthority(String authorities, String id);

    List<Authority> findByGroup(String name);

}
