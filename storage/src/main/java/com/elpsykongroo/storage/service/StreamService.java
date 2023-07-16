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

import com.elpsykongroo.base.domain.storage.object.S3;

import java.io.IOException;

public interface StreamService {
    String checkSha256(S3 s3);

    void uploadStream(String clientId, S3 s3, Integer num, String uploadId) throws IOException;

    void autoComplete(S3 s3);
}
