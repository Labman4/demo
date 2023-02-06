package com.elpsykongroo.auth.server.service.custom;

import com.elpsykongroo.auth.server.entity.client.Client;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ClientService {
    String add(Client client);

    String delete(String clientId);

    int update(Client client);

    List<Client> findAll();
}
