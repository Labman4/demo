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

package com.elpsykongroo.storage.client.impl;

import com.elpsykongroo.storage.client.StorageService;
import com.elpsykongroo.base.domain.storage.object.S3;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

@Component
public class StorageServiceImpl implements StorageService {
    private RestTemplate restTemplate;
    private String serverUrl = "http://localhost:9999";
    private String objectPrefix =  "/storage/object";

    public StorageServiceImpl(String serverUrl, RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 10809));
//        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
//        requestFactory.setProxy(proxy);
//        restTemplate.setRequestFactory(requestFactory);
        this.serverUrl = serverUrl;
    }

    @Override
    public ResponseEntity uploadObject (S3 s3) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
            form.add("data", s3.getData()[0].getResource());
            form.add("endpoint", s3.getEndpoint());
            form.add("accessKey", s3.getAccessKey());
            form.add("accessSecret", s3.getAccessSecret());
            form.add("bucket", s3.getBucket());
            form.add("region", s3.getRegion());
            form.add("key", s3.getKey());
            form.add("idToken", s3.getIdToken());
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(form, headers);
            restTemplate.exchange(serverUrl + objectPrefix,
                    HttpMethod.POST,
                    requestEntity,
                    String.class).getBody();
            return new ResponseEntity(HttpStatusCode.valueOf(200));
        } catch (RestClientException e) {
            return new ResponseEntity(HttpStatusCode.valueOf(500))    ;
        }
    }

    @Override
    public void downloadObject(S3 s3, HttpServletResponse response) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
        HttpEntity<S3> entity = new HttpEntity<S3>(s3, headers);
        ResponseEntity<byte[]> resp = restTemplate.exchange(
                serverUrl + objectPrefix + "/download",
                HttpMethod.POST,
                entity,
                byte[].class);

        byte[] fileBytes = resp.getBody();
        ByteArrayInputStream in = new ByteArrayInputStream(fileBytes);
        response.setHeader("Content-Disposition", "attachment; filename=" + s3.getKey());
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            byte[] b = new byte[1024];
            int len ;
            while ((len = in.read(b)) != -1) {
                out.write(b,0,len);
            }
        } finally {
            out.flush();
            out.close();
        }
    }

    @Override
    public void deleteObject(S3 s3) {
        restTemplate.postForObject(serverUrl + objectPrefix + "/delete", s3, String.class);
    }

    @Override
    public String listObject(S3 s3) {
        return restTemplate.postForObject(serverUrl + objectPrefix + "/list", s3, String.class);
    }
}
