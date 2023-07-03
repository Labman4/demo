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
import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.storage.service.ObjectService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
        return objectService.getUploadId(s3);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void multipartUpload(S3 s3) {
        try {
            if (!s3.getData()[0].isEmpty()) {
                objectService.multipartUpload(s3);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("multipart error: {}", e.getMessage());
            }
        }
    }

    @PostMapping("download")
    public void download(@RequestBody S3 s3, HttpServletResponse response) {
        try {
            objectService.download(s3, response);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("download error: {}", e.getMessage());
            }
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
            objectService.download(s3, response);
        } catch (IOException e) {
            if (log.isErrorEnabled()) {
                log.error("download error: {}", e.getMessage());
            }
        }
    }

    @PostMapping("list")
    public String list (@RequestBody S3 s3) {
        return CommonResponse.data(objectService.list(s3));
    }

    @PostMapping("delete")
    public void delete (@RequestBody S3 s3) { objectService.delete(s3); }
}
