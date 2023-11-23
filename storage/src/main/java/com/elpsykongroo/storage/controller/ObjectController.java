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

package com.elpsykongroo.storage.controller;

import com.elpsykongroo.base.common.CommonResponse;
import com.elpsykongroo.base.domain.message.Message;
import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.base.utils.MessageDigestUtils;
import com.elpsykongroo.storage.service.ObjectService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@CrossOrigin
@Slf4j
@RestController
@RequestMapping("storage/object")
public class ObjectController {

    @Autowired
    private ObjectService objectService;

    @PostMapping
    public String preUpload(@RequestBody S3 s3) throws IOException {
        return objectService.obtainUploadId(s3);
    }

    @PostMapping("abort")
    public void abortMultipartUpload(@RequestBody S3 s3) {
        objectService.abortMultipartUpload(s3);
    }

    @PostMapping("receive")
    public String receiveData(@RequestBody Message message) {
        try {
            return objectService.receiveMessage(message);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("receive message error: {}", e.getMessage());
            }
            return "0";
        }
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void multipartUpload(S3 s3) {
        try {
            if (s3.getData() != null && !s3.getData()[0].isEmpty()) {
                if (log.isDebugEnabled()) {
                    String sha256 = MessageDigestUtils.sha256(s3.getData()[0].getBytes());
                    log.debug("sha256:{}", sha256);
                }
                objectService.multipartUpload(s3);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("multipart file error: {}", e.getMessage());
            }
        }
    }

    @PostMapping(consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void uploadByte(S3 s3) {
        try {
            if (s3.getByteData() != null && s3.getByteData().length > 0) {
                objectService.multipartUpload(s3);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("multipart byte error: {}", e.getMessage());
            }
        }
    }

    @PostMapping("download")
    public void download(@RequestBody S3 s3, HttpServletRequest request, HttpServletResponse response) throws IOException {
        objectService.download(s3, request, response);
    }

    @GetMapping("url")
    public void getObjectByCode(@RequestParam String code,
                                @RequestParam String state,
                                @RequestParam String key,
                                @RequestParam(required = false) String offset,
                                @RequestParam(required = false) String secret,
                                HttpServletRequest request, HttpServletResponse response) throws IOException {
        objectService.getObjectByCode(code, state, key, offset, secret, request, response);
    }

    @PostMapping("url")
    public String getObjectUrl(@RequestBody S3 s3) throws IOException {
        return objectService.getObjectUrl(s3);
    }

    @PostMapping("list")
    public String list(@RequestBody S3 s3) {
        return CommonResponse.data(objectService.list(s3));
    }

    @PostMapping("delete")
    public void delete(@RequestBody S3 s3) { objectService.delete(s3); }

    @PostMapping("cors")
    public String getCors(@RequestBody S3 s3) { return CommonResponse.data(objectService.getCorsRule(s3)); }

    @PutMapping("cors")
    public void putCors(@RequestBody S3 s3) { objectService.putCorsRule(s3); }

}
