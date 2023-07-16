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

package com.elpsykongroo.storage.service;

import com.elpsykongroo.base.domain.message.Message;
import com.elpsykongroo.base.domain.storage.object.ListObjectResult;
import com.elpsykongroo.base.domain.storage.object.S3;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public interface ObjectService {
    String obtainUploadId(S3 s3) throws IOException;

    String multipartUpload(S3 s3) throws IOException;

    List<ListObjectResult> list(S3 s3);

    void download(S3 s3, HttpServletRequest request, HttpServletResponse response) throws IOException;

    void delete(S3 s3);

    String getObjectUrl(S3 s3) throws IOException;

    void getObjectByCode(String code, String state, String key, String offset, HttpServletRequest request, HttpServletResponse response) throws IOException;

    String receiveMessage(Message message) throws IOException;
}
