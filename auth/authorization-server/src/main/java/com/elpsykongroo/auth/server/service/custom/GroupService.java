package com.elpsykongroo.auth.server.service.custom;

import com.elpsykongroo.auth.server.entity.user.Authority;
import com.elpsykongroo.auth.server.entity.user.Group;

import java.util.List;

public interface GroupService {
    void addGroup(String group);

    int deleteGroup(String group);

    List<Group> groupList();

    List<Group> userGroup(String id);

    List<Group> findByAuthority(String name);

    int updateUserGroup(String groups, String id);
}
