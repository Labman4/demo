package com.elpsykongroo.auth.client;

import com.elpsykongroo.auth.client.dto.Client;


public interface AuthService {
    String add(Client client);

    void delete(String clientId);

    String findAll();
}
