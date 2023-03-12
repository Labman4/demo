package com.elpsykongroo.storage.client.impl;

import com.elpsykongroo.storage.client.StorageService;
import com.elpsykongroo.storage.client.dto.S3;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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
        this.serverUrl = serverUrl;
    }

    @Override
    public void uploadObject(S3 s3) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        System.out.println(s3.getData()[0].getBytes().length);
        form.add("data", s3.getData()[0].getResource());
        form.add("endpoint", s3.getEndpoint());
        form.add("accessKey", s3.getAccessKey());
        form.add("accessSecret", s3.getAccessSecret());
        form.add("bucket", s3.getBucket());
        form.add("region", s3.getRegion());
        form.add("key", s3.getKey());
        form.add("idToken", s3.getIdToken());
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(form, headers);
        restTemplate.exchange(serverUrl + objectPrefix + "/upload",
                HttpMethod.PUT,
                requestEntity,
                String.class).getBody();
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
