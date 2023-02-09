package com.elpsykongroo.auth.client.impl;

import com.elpsykongroo.auth.client.AuthService;
import com.elpsykongroo.auth.client.dto.Client;
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
    private String servicePrefix =  "/auth/client";

    public AuthServiceImpl(String serverUrl, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.serverUrl = serverUrl;
    }
    @Override
    public String add(String auth, Client client) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<Client> requestEntity = new HttpEntity(client, headers);
        return restTemplate.exchange(serverUrl + servicePrefix + "/add",
                HttpMethod.POST,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String delete(String auth, String clientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + servicePrefix + "/delete/" + clientId,
                HttpMethod.DELETE,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String findAll(String auth) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", auth);
        HttpEntity<String> requestEntity = new HttpEntity(headers);
        return restTemplate.exchange(serverUrl + servicePrefix + "/list",
                HttpMethod.GET,
                requestEntity,
                String.class).getBody();
//        return restTemplate.getForEntity(serverUrl + servicePrefix + "/list", String.class).getBody();
    }
}
