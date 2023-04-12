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

import com.elpsykongroo.storage.client.StorageService;
import com.elpsykongroo.storage.client.dto.S3;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("storage")
public class StorageController {
    @Autowired
    private StorageService storageService;

    @PutMapping("/object/upload")
    public void uploadObject(@ModelAttribute S3 s3) {
        try {
            storageService.uploadObject(s3);
        } catch (Exception e) {
            log.error("object upload multipart error: ", e);
        }
    }

    @GetMapping("/object/get")
    public void get(S3 s3, HttpServletResponse response) {
        try {
            storageService.downloadObject(s3, response);
        } catch (IOException e) {
            log.error("object download error: ", e);
        }
    }

    @PostMapping("/object/download")
    public void download(S3 s3, HttpServletResponse response) {
        try {
            storageService.downloadObject(s3, response);
        } catch (IOException e) {
            log.error("object download error: ", e);
        }
    }

    @PostMapping("/object/list")
    public String list (@RequestBody S3 s3) {
        return storageService.listObject(s3);
    }

    @PostMapping("/object/delete")
    public void delete (@RequestBody S3 s3) { storageService.deleteObject(s3); }
}
