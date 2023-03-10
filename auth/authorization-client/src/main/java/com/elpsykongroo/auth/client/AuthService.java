package com.elpsykongroo.auth.client;

import com.elpsykongroo.auth.client.dto.Client;
import com.elpsykongroo.auth.client.dto.ClientRegistry;


public interface AuthService {
    String addClient(String auth, Client client);

    String deleteClient(String auth, String clientId);

    String findAllClient(String auth);

    String addRegister(String auth, ClientRegistry client);

    String deleteRegister(String auth, String clientId);

    String findAllRegister(String auth);
}
