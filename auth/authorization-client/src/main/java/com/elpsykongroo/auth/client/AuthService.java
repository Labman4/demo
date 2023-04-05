package com.elpsykongroo.auth.client;

import com.elpsykongroo.auth.client.dto.Client;
import com.elpsykongroo.auth.client.dto.ClientRegistry;
import com.elpsykongroo.auth.client.dto.User;
import com.elpsykongroo.auth.client.dto.UserInfo;


public interface AuthService {
    String addClient(String auth, Client client);

    String deleteClient(String auth, String clientId);

    String findAllClient(String auth);

    String addRegister(String auth, ClientRegistry client);

    String deleteRegister(String auth, String clientId);

    String findAllRegister(String auth);

    String addGroup(String auth, String name);

    String addAuthority(String auth, String name);

    String authorityList(String auth);

    String groupList(String auth);

    String deleteGroup(String auth, String name);

    String deleteAuthority(String auth, String name);

    String loadUserInfo(String auth, String username);

    String updateUserInfo(String auth, UserInfo userinfo);

    String updateUser(String auth, User user);

    String updateUserGroup(String auth, String groups, String ids);

    String updateUserAuthority(String auth, String authorities, String id);

    String updateGroupAuthority(String auth, String authorities, String id);

    String userList(String auth, String pageNumber, String pageSize, String order);

    String userAuthority(String auth, String id);

    String userGroup(String auth, String id);

    String groupAuthorityList(String auth, String name);

    String authorityGroupList(String auth, String name);

}
