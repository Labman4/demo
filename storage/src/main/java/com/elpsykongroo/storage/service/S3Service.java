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
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.util.List;

public interface S3Service {
    String uploadObject(String clientId, String bucket, String key, RequestBody requestBody);

    void deleteObject(String clientId, String bucket, String key);

    void deleteObjects(String clientId, String bucket, String keys);

    void deleteObjectByPrefix(String clientId, String bucket, String prefix);

    String getObject(String clientId, String bucket, String key);

    ResponseInputStream<GetObjectResponse> getObjectStream(String clientId, String bucket, String key, String offset);

    ListObjectsV2Iterable listObject(String clientId, S3Client s3Client, String bucket, String prefix);

    CreateMultipartUploadResponse createMultiPart(String clientId, String bucket, String key);

    boolean createBucket(String clientId, String platform, String bucket);

    HeadObjectResponse headObject(String clientId, String bucket, String key);

    UploadPartResponse uploadPart(String clientId, S3 s3, RequestBody requestBody, int partNum, long endOffset) throws IOException;

    ListMultipartUploadsResponse listMultipartUploads(String clientId, S3Client s3Client, String platform, String bucket);

    void listCompletedPart(String clientId, String bucket, String key, String uploadId, List<CompletedPart> completedParts);

    ListPartsResponse listParts(String clientId, String bucket, String key, String uploadId);

    void completePart(String clientId, String bucket, String key, String uploadId, List<CompletedPart> completedParts);

    S3Client initClient(S3 s3, String clientId);

    void abortMultipartUpload (String clientId, String bucket, String key, String uploadId);
}
