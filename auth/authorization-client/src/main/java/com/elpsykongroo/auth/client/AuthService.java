package com.elpsykongroo.auth.client;

import com.elpsykongroo.auth.client.dto.Client;


public interface AuthService {
    String add(String auth, Client client);

    String delete(String auth, String clientId);

    String findAll(String auth);
}
