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

package com.elpsykongroo.base.domain.storage.object;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
public class S3 {
    private String accessKey;

    private String accessSecret;

    private String region;

    private String endpoint;

    private String bucket;

    private String key;

    private MultipartFile data[];

    private byte[] byteData;

    private String partSize = "5242880";

    private String offset;

    /*
    use for sts
     */
    private String idToken;

    private String mode;

    private String partCount;

    private String partNum;

    private String uploadId;

    private String consumerGroupId;

    private String platform;

    private String sha256;

    private String clientId;

    public S3(String bucket) {
        this.bucket = bucket;
    }
}

