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

package com.elpsykongroo.gateway.controller;

import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.base.service.StorageService;
import feign.Response;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;

@CrossOrigin
@Slf4j
@RestController
@RequestMapping("storage/object")
public class StorageController {
    @Autowired
    private StorageService storageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadObject(S3 s3) {
        try {
            storageService.uploadObject(s3);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping
    public void getObject(@RequestParam String bucket,
                          @RequestParam String key,
                          @RequestParam(required = false) String idToken,
                          HttpServletResponse response) {
        try {
            S3 s3 = new S3();
            s3.setBucket(bucket);
            s3.setKey(key);
            s3.setIdToken(idToken);
            Response feginResp = storageService.downloadObject(s3);
            InputStream in = feginResp.body().asInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
            response.setContentType("multipart.form-data");
            response.setHeader("Content-Disposition", feginResp.headers().get("Content-Disposition").toString().replace("[","").replace("]",""));
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(response.getOutputStream());
            int length;
            byte[] temp = new byte[1024 * 10];
            while ((length = bufferedInputStream.read(temp)) != -1) {
                bufferedOutputStream.write(temp, 0, length);
            }
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            bufferedInputStream.close();
            in.close();
        } catch (IOException e) {
            log.error("object download error: {}", e);
        }
    }

    @PostMapping("download")
    public void download(S3 s3, HttpServletResponse response) {
        try {
            Response feginResp = storageService.downloadObject(s3);
            InputStream in = feginResp.body().asInputStream();
            BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
            response.setContentType("multipart.form-data");
            response.setHeader("Content-Disposition", feginResp.headers().get("Content-Disposition").toString().replace("[","").replace("]",""));
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(response.getOutputStream());
            int length;
            byte[] temp = new byte[1024 * 10];
            while ((length = bufferedInputStream.read(temp)) != -1) {
                bufferedOutputStream.write(temp, 0, length);
            }
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            bufferedInputStream.close();
            in.close();
        } catch (IOException e) {
            log.error("object download error: {}", e);
        }
    }

    @PostMapping("list")
    public String list(S3 s3) {
        return storageService.listObject(s3);
    }

    @PostMapping("delete")
    public void delete(S3 s3) { storageService.deleteObject(s3); }
}
