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

package com.elpsykongroo.base.service;

import com.elpsykongroo.base.domain.storage.object.S3;
import feign.Headers;
import feign.RequestLine;
import feign.Response;

import java.io.IOException;

public interface StorageService {

    @RequestLine("POST /storage/object")
    @Headers({
            "Content-Type: application/json"
    })
    String createMultiPart(S3 s3);

    @RequestLine("POST /storage/object")
    @Headers({
            "Content-Type: multipart/form-data"
    })
    void uploadObject(S3 s3);

    @RequestLine("POST /storage/object/download")
    @Headers({
            "Content-Type: application/json"
    })
    Response downloadObject(S3 s3) throws IOException;

    @RequestLine("POST /storage/object/delete")
    @Headers({
            "Content-Type: application/json"
    })
    void deleteObject(S3 s3);

    @RequestLine("POST /storage/object/list")
    @Headers({
            "Content-Type: application/json"
    })
    String listObject(S3 s3);
}
