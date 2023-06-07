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

package com.elpsykongroo.storage.server.controller;

import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.storage.server.service.ObjectService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/storage/object")
public class ObjectController {
    @Autowired
    private ObjectService objectService;

    @CrossOrigin
    @PostMapping(consumes= { MediaType.MULTIPART_FORM_DATA_VALUE })
    public void multipartUpload(@RequestPart S3 s3, @RequestPart("file") MultipartFile data) {
        try {
            objectService.multipartUpload(s3, data);
        } catch (Exception e) {
            log.error("multipart error: ", e);
        }
    }

    @PostMapping("download")
    public void download(@RequestBody S3 s3, HttpServletResponse response) {
        try {
            objectService.download(s3, response);
        } catch (IOException e) {
            log.error("download error");
        }
    }

    @PostMapping("list")
    public String list (@RequestBody S3 s3) {
        return JsonUtils.toJson(objectService.list(s3));
    }

    @PostMapping("delete")
    public void delete (@RequestBody S3 s3) { objectService.delete(s3); }
}
