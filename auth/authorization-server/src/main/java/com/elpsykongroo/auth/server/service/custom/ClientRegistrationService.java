package com.elpsykongroo.auth.server.service.custom;

import com.elpsykongroo.auth.server.entity.client.ClientRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ClientRegistrationService {

    String add(ClientRegistry client);

    String delete(String registryId);

    List<ClientRegistry> findAll();

    int update(ClientRegistry client);
}
