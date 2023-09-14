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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@CrossOrigin
@Slf4j
@RestController
@RequestMapping("storage/object")
public class StorageController {
    @Autowired
    private StorageService storageService;

    @PostMapping
    public String preUpload(@RequestBody S3 s3) {
        return storageService.createMultiPart(s3);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadFile(S3 s3) {
        if ((s3.getData() != null && !s3.getData()[0].isEmpty())) {
            storageService.uploadFile(s3);
        }
    }

    @PostMapping("/abort")
    public void abortMultipartUpload(@RequestBody S3 s3) {
        storageService.abortMultiPart(s3);
    }

    @PostMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void uploadByte( byte[] byteData, @RequestBody S3 s3) {
        if ((s3.getByteData() != null && s3.getByteData().length > 0)) {
            storageService.uploadByte(s3);
        }
    }

    @PostMapping("url")
    public String getObjectUrl(@RequestBody S3 s3) throws IOException {
        return storageService.getObjectUrl(s3);
    }

    @GetMapping("url")
    public void getObjectByCode(@RequestParam String code,
                                @RequestParam String key,
                                @RequestParam String state,
                                HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        Response feignResp = storageService.getObjectByCode(code, state, key, getRange(request, response));
        write(response, feignResp);
        Map<String, String> result = new HashMap<>();
        Iterator iterator = response.getHeaderNames().iterator();
        while (iterator.hasNext()) {
            String header = (String) iterator.next();
            String value = response.getHeader(header);
            result.put(header, value);
        }
        if (log.isDebugEnabled()) {
            log.debug("getObjectByCode resp header:{}", result);
        }
    }

    @GetMapping
    public void getObjectByDefault(@RequestParam String bucket,
                                   @RequestParam String key,
                                   @RequestParam(required = false) String idToken,
                                   HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {
        S3 s3 = new S3();
        s3.setBucket(bucket);
        s3.setKey(key);
        s3.setIdToken(idToken);
        s3.setOffset(getRange(request, response));
        Response feignResp = storageService.downloadObject(s3);
        write(response, feignResp);
    }

    @PostMapping("download")
    public void download(@RequestBody S3 s3, HttpServletRequest request, HttpServletResponse response) throws IOException {
        s3.setOffset(getRange(request, response));
        Response feignResp = storageService.downloadObject(s3);
        write(response, feignResp);
    }

    @PostMapping("list")
    public String list(@RequestBody S3 s3) {
        return storageService.listObject(s3);
    }

    @PostMapping("delete")
    public void delete(@RequestBody S3 s3) {
        storageService.deleteObject(s3);
    }

    private String getRange(HttpServletRequest request, HttpServletResponse response) {
        String range = request.getHeader("Range");
        if (StringUtils.isNotBlank(range)) {
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            String[] ranges = range.replace("bytes=", "").split("-");
            return ranges[0];
        }
        return "";
    }

    private void write(HttpServletResponse response, Response feignResp) throws IOException {
        InputStream in = null;
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;
        try {
            in = feignResp.body().asInputStream();
            if (in != null) {
                bufferedInputStream = new BufferedInputStream(in);
                for (Map.Entry entry: feignResp.headers().entrySet()) {
                    String header = (String) entry.getKey();
                    String value = entry.getValue().toString().replace("[", "").replace("]", "");
                    response.addHeader(header, value);
                }
                bufferedOutputStream = new BufferedOutputStream(response.getOutputStream());
                int length;
                byte[] temp = new byte[1024 * 10];
                while ((length = bufferedInputStream.read(temp)) != -1) {
                    bufferedOutputStream.write(temp, 0, length);
                }
            }
        } finally {
            if (bufferedOutputStream != null) {
                bufferedOutputStream.flush();
                bufferedOutputStream.close();
            }
            if (bufferedInputStream != null) {
                bufferedInputStream.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }
}
