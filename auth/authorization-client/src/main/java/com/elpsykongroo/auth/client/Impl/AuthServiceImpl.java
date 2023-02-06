package com.elpsykongroo.auth.client.impl;

import com.elpsykongroo.auth.client.AuthService;
import com.elpsykongroo.auth.client.dto.Client;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Component
public class AuthServiceImpl implements AuthService {
    private RestTemplate restTemplate;
    private String serverUrl = "http://localhost:9000";
    private String servicePrefix =  "/auth/client";

    public AuthServiceImpl(String serverUrl, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.serverUrl = serverUrl;
    }
    @Override
    public String add(Client client) {
        return restTemplate.postForEntity(serverUrl + servicePrefix + "/add", client, String.class).getBody();
    }

    @Override
    public void delete(String clientId) {
        restTemplate.delete(serverUrl + servicePrefix + "/delete/" + clientId);
    }

    @Override
    public String findAll() {
        return restTemplate.getForEntity(serverUrl + servicePrefix + "/list", String.class).getBody();
    }
}
