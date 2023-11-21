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
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.PutBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.IOException;
import java.util.List;

public interface S3Service {
    String uploadObject(S3Client s3Client, String bucket, String key, RequestBody requestBody);

    void deleteObject(S3Client s3Client, String bucket, String key);

    void deleteObjects(S3Client s3Client, String bucket, String keys);

    void deleteObjectByPrefix(S3Client s3Client, String bucket, String prefix);

    ResponseBytes<GetObjectResponse> getObject(S3Client s3Client, String bucket, String key);

    String getObjectString(S3Client s3Client, String bucket, String key);

    GetBucketCorsResponse getCorsRule(S3Client s3Client, String bucket);

    PutBucketCorsResponse putCorsRule(S3Client s3Client, String bucket, List<CORSRule> corsRules);

    ResponseInputStream<GetObjectResponse> getObjectStream(S3Client s3Client, String bucket, String key, String offset);

    ListObjectsV2Iterable listObject(S3Client s3Client, String bucket, String prefix);

    CreateMultipartUploadResponse createMultiPart(S3Client s3Client, String bucket, String key);

    boolean createBucket(S3Client s3Client, String platform, String bucket);

    HeadObjectResponse headObject(S3Client s3Client, String bucket, String key);

    UploadPartResponse uploadPart(S3Client s3Client, S3 s3, RequestBody requestBody, int partNum, long endOffset) throws IOException;

    ListMultipartUploadsResponse listMultipartUploads(S3Client s3Client, String platform, String bucket);

    void listCompletedPart(S3Client s3Client, String bucket, String key, String uploadId, List<CompletedPart> completedParts);

    ListPartsResponse listParts(S3Client s3Client, String bucket, String key, String uploadId);

    void completePart(S3Client s3Client, String bucket, String key, String uploadId, List<CompletedPart> completedParts);

    S3Client initClient(S3 s3, String clientId);

    void abortMultipartUpload (S3Client s3Client, String bucket, String key, String uploadId);
}
