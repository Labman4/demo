package com.elpsykongroo.auth.client.impl;

import com.elpsykongroo.auth.client.AuthService;
import com.elpsykongroo.auth.client.dto.Client;
import com.elpsykongroo.auth.client.dto.ClientRegistry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AuthServiceImpl implements AuthService {

    private RestTemplate restTemplate;

    private String serverUrl = "http://localhost:9000";

    private String clientPrefix =  "/auth/client";

    private String registryPrefix =  "/auth/client/register";

    public AuthServiceImpl(String serverUrl, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.serverUrl = serverUrl;
    }
    @Override
    public String addClient(String auth, Client client) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<Client> requestEntity = new HttpEntity(client, headers);
        return restTemplate.exchange(serverUrl + clientPrefix + "/add",
                HttpMethod.POST,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String deleteClient(String auth, String clientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + clientPrefix + "/delete/" + clientId,
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String findAllClient(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + clientPrefix + "/list",
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();
//        return restTemplate.getForEntity(serverUrl + servicePrefix + "/list", String.class).getBody();
    }

    @Override
    public String addRegister(String auth, ClientRegistry client) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<Client> requestEntity = new HttpEntity(client, headers);
        return restTemplate.exchange(serverUrl + registryPrefix + "/add",
                HttpMethod.POST,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String deleteRegister(String auth, String clientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + registryPrefix + "/delete/" + clientId,
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();    }

    @Override
    public String findAllRegister(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + registryPrefix + "/list",
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();
    }
}
