/*
 * Copyright 2022-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elpsykongroo.services.elasticsearch.client.impl;

import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.services.elasticsearch.client.SearchService;
import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecord;
import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecordDto;
import com.elpsykongroo.services.elasticsearch.client.dto.IPManage;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class SearchServiceImpl implements SearchService {
    private RestTemplate restTemplate;
    private String serverUrl = "http://localhost:9201";
    private String recordPrefix =  "/search/record";
    private String ipPrefix =  "/search/ip";

    public SearchServiceImpl() {

    }

    public SearchServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public SearchServiceImpl(String serverUrl, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
        this.serverUrl = serverUrl;
    }
    @Override
    public String findAllIP(String pageNumber, String pageSize) {
        return restTemplate.exchange(serverUrl + ipPrefix
                        + "/list?pageNumber=" + pageNumber
                        + "&pageSize=" + pageSize,
                HttpMethod.GET,
                null,
                String.class).getBody();
    }

    @Override
    public List<AccessRecord> searchSimilar(AccessRecordDto accessRecord) {
        HttpEntity<AccessRecordDto> requestEntity = new HttpEntity(accessRecord);
//         ResponseEntity<List<AccessRecord>> response = restTemplate.exchange(
//                   serverUrl + recordPrefix + "/filter",
//                    HttpMethod.POST,
//                    requestEntity,
//                    new ParameterizedTypeReference<List<AccessRecord>>() {
//                    });
        String records = restTemplate.exchange(
                   serverUrl + recordPrefix + "/filter",
                    HttpMethod.POST,
                    requestEntity,
                    String.class).getBody();
        return JsonUtils.toType(records, new TypeReference<List<AccessRecord>>() {});
    }

    @Override
    public List<IPManage> findByIsBlackTrue() {
//        ResponseEntity<List<IPManage>> response = restTemplate.exchange(
//                serverUrl + ipPrefix + "/black/list",
//                HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<IPManage>>() {});
//        return response.getBody();
        String ips = restTemplate.exchange(
                serverUrl + ipPrefix + "/black/list",
                HttpMethod.GET,
                null,
                String.class).getBody();
        return JsonUtils.toType(ips, new TypeReference<List<IPManage>>() {});
    }

    @Override
    public List<IPManage> findByIsBlackFalse() {
//        ResponseEntity<List<IPManage>> response = restTemplate.exchange(
//                serverUrl + ipPrefix + "/white/list",
//                HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<IPManage>>() {});
//        return response.getBody();
        String ips = restTemplate.exchange(
                serverUrl + ipPrefix + "/white/list",
                HttpMethod.GET,
                null,
                String.class).getBody();
        return JsonUtils.toType(ips, new TypeReference<List<IPManage>>() {});
    }

    @Override
    public String findByIsBlackTrue(String pageNumber,
                                            String pageSize) {
//        ResponseEntity<List<IPManage>> response = restTemplate.exchange(
//                serverUrl + ipPrefix
//                        + "/black/list?pageNumber=" + pageNumber
//                        + "&pageSize=" + pageSize,
//                HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<IPManage>>() {});
        return restTemplate.exchange(
                serverUrl + ipPrefix
                        + "/black/list?pageNumber=" + pageNumber
                        + "&pageSize=" + pageSize,                HttpMethod.GET,
                null,
                String.class).getBody();
    }

    @Override
    public String findByIsBlackFalse(String pageNumber,
                                             String pageSize) {
//        ResponseEntity<List<IPManage>> response = restTemplate.exchange(
//                serverUrl + ipPrefix
//                        + "/white/list?pageNumber=" + pageNumber
//                        + "&pageSize=" + pageSize,
//                HttpMethod.GET,
//               null,
//               new ParameterizedTypeReference<List<IPManage>>() {});
//        return response.getBody();
         return restTemplate.exchange(
                serverUrl + ipPrefix
                        + "/white/list?pageNumber=" + pageNumber
                        + "&pageSize=" + pageSize,                HttpMethod.GET,
                null,
                String.class).getBody();
    }

    @Override
    public String countByAddressAndIsBlackTrue(String address) {
        return restTemplate.exchange(serverUrl + ipPrefix + "/count/black/" + address,
                HttpMethod.GET,
                null,
                String.class).getBody();    }

    @Override
    public String countByAddressAndIsBlackFalse(String address) {
        return restTemplate.exchange(serverUrl + ipPrefix + "/count/white/" + address,
                HttpMethod.GET,
                null,
                String.class).getBody();
    }

    @Override
    public void deleteByAddressAndIsBlackTrue(String address) {
        restTemplate.exchange(serverUrl + ipPrefix + "/black/delete/" + address,
                HttpMethod.DELETE,
                null,
                String.class).getBody();
    }

    @Override
    public void deleteByAddressAndIsBlackFalse(String address) {
        restTemplate.exchange(serverUrl + ipPrefix + "/white/delete/" + address,
                HttpMethod.DELETE,
                null,
                String.class).getBody();
    }

    @Override
    public String findAllRecord(String pageNumber,
                                String pageSize,
                                String order) {
        return restTemplate.exchange(serverUrl + recordPrefix
                        + "/list?pageNumber=" + pageNumber
                        + "&pageSize=" + pageSize
                        + "&order=" + order,
                HttpMethod.GET,
                null,
                String.class).getBody();
    }

    @Override
    public void saveRecord(AccessRecord accessrecord) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AccessRecord> requestEntity = new HttpEntity(accessrecord, headers);
        restTemplate.exchange(serverUrl + recordPrefix + "/add",
                HttpMethod.PUT,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public String saveIP(IPManage ipManage) {
        HttpEntity<IPManage> requestEntity = new HttpEntity(ipManage);
        return restTemplate.exchange(serverUrl + ipPrefix + "/add",
                HttpMethod.PUT,
                requestEntity,
                String.class).getBody();
    }


    @Override
    public List<AccessRecord> findBySourceIP(String sourceip) {
//        ResponseEntity<List<AccessRecord>> response = restTemplate.exchange(
//                serverUrl + recordPrefix + "/list/ip?ip=" + sourceip,
//                HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<AccessRecord>>() {});
//        return response.getBody();
        String records = restTemplate.exchange(
                serverUrl + recordPrefix + "/list/ip?ip=" + sourceip,
                HttpMethod.GET,
                null,
                String.class).getBody();
        return JsonUtils.toType(records, new TypeReference<List<AccessRecord>>() {});
    }

    @Override
    public List<AccessRecord> findByAccessPathLike(String path) {
//        ResponseEntity<List<AccessRecord>> response = restTemplate.exchange(
//                serverUrl + recordPrefix + "/list/path?path=" + path,
//                HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<AccessRecord>>() {});
//        return response.getBody();
        String records = restTemplate.exchange(
                serverUrl + recordPrefix + "/list/path?path=" + path,
                HttpMethod.GET,
                null,
                String.class).getBody();
        return JsonUtils.toType(records, new TypeReference<List<AccessRecord>>() {});
    }

    @Override
    public List<AccessRecord> findByUserAgentLike(String agent) {
//        ResponseEntity<List<AccessRecord>> response = restTemplate.exchange(
//                serverUrl + recordPrefix + "/list/agent?agent=" + agent,
//                HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<AccessRecord>>() {
//                });
        String records = restTemplate.exchange(
                serverUrl + recordPrefix + "/list/agent?agent=" + agent,
                HttpMethod.GET,
                null,
                String.class).getBody();
        return JsonUtils.toType(records, new TypeReference<List<AccessRecord>>() {});
    }

    @Override
    public List<AccessRecord> findByRequestHeaderLike(String header) {
//        ResponseEntity<List<AccessRecord>> response = restTemplate.exchange(
//                serverUrl + recordPrefix + "/list/header?header=" + header,
//                HttpMethod.GET,
//                null,
//                new ParameterizedTypeReference<List<AccessRecord>>() {});
        String records = restTemplate.exchange(
                serverUrl + recordPrefix + "/list/header?header=" + header,
                HttpMethod.GET,
                null,
                String.class).getBody();
        return JsonUtils.toType(records, new TypeReference<List<AccessRecord>>() {});
    }

    @Override
    public void deleteAllRecordById(List<String> ids) {
        HttpEntity<List<String>> requestEntity = new HttpEntity(ids);
        restTemplate.exchange(serverUrl + recordPrefix + "/delete",
                HttpMethod.POST,
                requestEntity,
                String.class).getBody();
    }

    @Override
    public void deleteIPById(String id) {
        restTemplate.exchange(serverUrl + ipPrefix + "/delete/" + id,
                HttpMethod.DELETE,
                null,
                String.class).getBody();
    }
}
