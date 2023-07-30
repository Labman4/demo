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

package com.elpsykongroo.storage.service.impl;

import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.storage.service.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.waiters.WaiterResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.InvalidObjectStateException;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.Credentials;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class S3ServiceImpl implements S3Service {

    public final Map<String, S3Client> clientMap = new ConcurrentHashMap<>();

    private final Map<String, String> stsClientMap = new ConcurrentHashMap<>();
    @Autowired
    public ServiceConfig serviceconfig;

    @Override
    public String uploadObject(String clientId, String bucket, String key, RequestBody requestBody) {
        if (log.isDebugEnabled()) {
            log.debug("uploadObject key:{}", key);
        }
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return clientMap.get(clientId).putObject(objectRequest, requestBody).eTag();
    }

    @Override
    public void deleteObject(String clientId, String bucket, String key) {
        if (log.isDebugEnabled()) {
            log.debug("deleteObject key:{}", key);
        }
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        clientMap.get(clientId).deleteObject(deleteObjectRequest);
    }

    @Override
    public void deleteObjects(String clientId, String bucket, String keys) {
        if (log.isDebugEnabled()) {
            log.debug("deleteObjects key:{}", keys);
        }
        List<ObjectIdentifier> toDelete = new ArrayList<>();
        for (String key: keys.split(",")) {
            toDelete.add(ObjectIdentifier.builder()
                    .key(key)
                    .build());
        }
        DeleteObjectsRequest deleteObjectRequest = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(toDelete).build())
                .build();
        clientMap.get(clientId).deleteObjects(deleteObjectRequest);
    }


    @Override
    public void deleteObjectByPrefix(String clientId, String bucket, String prefix) {
        List<ObjectIdentifier> toDelete = new ArrayList<>();
        listObject(clientId, bucket, prefix).contents().stream().forEach(obj -> toDelete.add(ObjectIdentifier.builder()
                .key(obj.key()).build()));
        DeleteObjectsRequest deleteObjectRequest = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(toDelete).build())
                .build();
        clientMap.get(clientId).deleteObjects(deleteObjectRequest);
    }

    @Override
    public String getObject(String clientId, String bucket, String key) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> bytesResp = clientMap.get(clientId).getObjectAsBytes(objectRequest);
            if (bytesResp != null) {
                String str = new String(bytesResp.asByteArray());
                if (log.isTraceEnabled()) {
                    log.trace("getObjectAsBytes value:{}",str);
                }
                return str;
            }
        } catch (NoSuchKeyException e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectAsBytes error:{} key:{}", e.getMessage(), key);
            }
            return null;
        } catch (SdkClientException e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectAsBytes client error:{}", e.getMessage());
            }
            return "";
        } catch (InvalidObjectStateException e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectAsBytes state error:{}", e.getMessage());
            }
            return "";
        } catch (S3Exception e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectAsBytes s3 error:{}", e.getMessage());
            }
            return "";
        }
        return "";
    }

    @Override
    public ResponseInputStream<GetObjectResponse> getObjectStream(String clientId, String bucket, String key, String offset) {
        try {
            GetObjectRequest objectRequest = null;
            GetObjectRequest.Builder builder = GetObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(key);
            if (StringUtils.isNotBlank(offset)) {
                long startPoint = Long.parseLong(offset);
                objectRequest = builder.range("bytes=" + startPoint + "-").build();
            } else {
                objectRequest = builder.build();
            }
            ResponseInputStream<GetObjectResponse> streamResp = clientMap.get(clientId).getObject(objectRequest);
            if (streamResp != null) {
                return streamResp;
            }
        } catch (NoSuchKeyException e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectStream error:{} key:{}", e.getMessage(), key);
            }
            return null;
        } catch (SdkClientException e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectStream client error:{}", e.getMessage());
            }
            return null;
        } catch (InvalidObjectStateException e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectStream state error:{}", e.getMessage());
            }
            return null;
        } catch (S3Exception e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectStream s3 error:{}", e.getMessage());
            }
            return null;
        }
        return null;
    }

    @Override
    public ListObjectsV2Iterable listObject(String clientId, String bucket, String prefix) {
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();
        return clientMap.get(clientId).listObjectsV2Paginator(listReq);
    }

    @Override
    public CreateMultipartUploadResponse createMultiPart(String clientId, String bucket, String key) {
        if (log.isTraceEnabled()) {
            log.trace("create multipartUpload");
        }
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return clientMap.get(clientId).createMultipartUpload(createMultipartUploadRequest);
    }

    @Override
    public boolean createBucket(String clientId, String platform, String bucket) {
        try {
            CreateBucketConfiguration createBucketConfiguration = CreateBucketConfiguration.builder().build();
            if (StringUtils.isNotBlank(platform) && "cloudflare".equals(platform)) {
                createBucketConfiguration = CreateBucketConfiguration.builder().locationConstraint("auto").build();
            }
            S3Waiter s3Waiter = clientMap.get(clientId).waiter();
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bucket)
                    .createBucketConfiguration(createBucketConfiguration)
                    .build();

            clientMap.get(clientId).createBucket(bucketRequest);
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();

            WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
            return waiterResponse.matched().response().isPresent();
        } catch (S3Exception e) {
            if (log.isErrorEnabled()) {
                log.error("create bucket error: {}", e.awsErrorDetails().errorMessage());
            }
        }
        return false;
    }

    @Override
    public HeadObjectResponse headObject(String clientId, String bucket, String key) {
        if (log.isTraceEnabled()) {
            log.trace("headObject clientId:{}, bucket:{}, key:{}", clientId, bucket, key);
        }
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            return clientMap.get(clientId).headObject(headObjectRequest);
        } catch (NoSuchKeyException e) {
            if (log.isTraceEnabled()) {
                log.trace("object not exist");
            }
            return null;
        } catch (S3Exception e) {
            if (log.isErrorEnabled()) {
                log.error("headObject error:{}", e.getMessage());
            }
            return null;
        }
    }

    @Override
    public UploadPartResponse uploadPart(String clientId, S3 s3, RequestBody requestBody, int partNum, long endOffset) throws IOException {
        UploadPartRequest uploadRequest = null;
        try {
            uploadRequest = UploadPartRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .uploadId(s3.getUploadId())
                    .partNumber(partNum)
                    .contentLength(endOffset)
                    .build();
            return clientMap.get(clientId).uploadPart(uploadRequest, requestBody);
        } catch (NoSuchUploadException e) {
            return null;
        } catch (SdkClientException e) {
            return clientMap.get(clientId).uploadPart(uploadRequest, requestBody);
        }
    }

    @Override
    public ListMultipartUploadsResponse listMultipartUploads(String clientId, String bucket) {
        ListMultipartUploadsRequest listMultipartUploadsRequest = ListMultipartUploadsRequest.builder()
                .bucket(bucket)
                .build();
        ListMultipartUploadsResponse resp = clientMap.get(clientId).listMultipartUploads(listMultipartUploadsRequest);
        if (log.isDebugEnabled()) {
            log.debug("listMultipartUploads: {}", resp.uploads().size());
        }
        return resp;
    }

//    public void abortMultipartUpload (String bucket, String key, String uploadId) {
//        AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
//                        .bucket(bucket)
//                        .key(key)
//                        .uploadId(uploadId)
//                        .build();
//        s3Client.abortMultipartUpload(abortMultipartUploadRequest);
//    }

    @Override
    public void listCompletedPart(String clientId, String bucket, String key, String uploadId, List<CompletedPart> completedParts) {
        ListPartsResponse listPartsResponse = null;
        try {
            listPartsResponse = listParts(clientId, bucket, key, uploadId);
        } catch (AwsServiceException e) {
            if (log.isErrorEnabled()) {
                log.error("listPart awsService error: {}", e.awsErrorDetails());
                completedParts.add(CompletedPart.builder()
                        .partNumber(0)
                        .build());
            }
        }
        if (listPartsResponse != null && listPartsResponse.parts().size() > 0) {
            for (Part part: listPartsResponse.parts()) {
                completedParts.add(CompletedPart.builder()
                        .partNumber(part.getValueForField("PartNumber", Integer.class).get())
                        .eTag(part.getValueForField("ETag", String.class).get())
                        .build());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("listCompletedPart: {}, part: {}", completedParts.size(), completedParts);
        }
    }

    @Override
    public ListPartsResponse listParts(String clientId, String bucket, String key, String uploadId) {
        if (log.isDebugEnabled()) {
            log.debug("list parts uploadId:{}", uploadId);
        }
        try {
            ListPartsRequest listRequest = ListPartsRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .build();
            return clientMap.get(clientId).listParts(listRequest);
        } catch (SdkClientException e) {
            if (log.isErrorEnabled()) {
                log.error("listPart sdk client error: {}", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void completePart(String clientId, String bucket, String key, String uploadId, List<CompletedPart> completedParts) {
        if (log.isInfoEnabled()) {
            log.info("start complete part");
        }
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .uploadId(uploadId)
                .multipartUpload(
                        CompletedMultipartUpload.builder()
                                .parts(completedParts)
                                .build()
                )
                .build();
        CompleteMultipartUploadResponse response = clientMap.get(clientId).completeMultipartUpload(completeRequest);
        if (log.isInfoEnabled()) {
            log.info("complete MultipartUpload: {}", response.eTag());
        }
    }

    @Override
    public void initClient(S3 s3, String clientId) {
        if (StringUtils.isBlank(s3.getPlatform())) {
            s3.setPlatform(serviceconfig.getS3().getPlatform());
        }

        if (StringUtils.isBlank(s3.getRegion())) {
            s3.setRegion(serviceconfig.getS3().getRegion());
        }

        if (StringUtils.isBlank(clientId)) {
            clientId = s3.getPlatform() + "*" + s3.getRegion() + "*" + s3.getBucket();
            s3.setClientId(clientId);
        }
        if (log.isDebugEnabled()) {
            log.debug("clientMap before:{}", clientMap.keySet());
        }
        if (clientMap.containsKey(clientId)) {
            if (!stsClientMap.containsKey(clientId + "-timestamp")) {
                if (log.isTraceEnabled()) {
                    log.trace("skip init");
                }
                return;
            } else {
                String timestamp = stsClientMap.get(clientId + "-timestamp");
                if (log.isDebugEnabled()) {
                    log.debug("client expired time :{}", timestamp);
                }
                if (Instant.now().compareTo(Instant.ofEpochMilli(Long.parseLong(timestamp)*1000)) < 0) {
                    return;
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("client expired, continue init");
                    }
                }
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("start init");
            }
        }

        if (StringUtils.isBlank(s3.getEndpoint())) {
            s3.setEndpoint(serviceconfig.getS3().getEndpoint());
        }

        Long connect = serviceconfig.getTimeout().getConnect();
        Duration connectDuration = Duration.ofSeconds(connect);
        SdkHttpClient.Builder builder = ApacheHttpClient.builder()
                .connectionTimeout(connectDuration)
                .socketTimeout(connectDuration)
                .proxyConfiguration(ProxyConfiguration.builder()
                        .useSystemPropertyValues(true)
                        .build())
                .connectionAcquisitionTimeout(connectDuration)
                .connectionMaxIdleTime(connectDuration)
                .connectionTimeToLive(connectDuration);
        AwsCredentials awsCredentials =
                AwsBasicCredentials.create(
                        serviceconfig.getS3().getAccessKey(),
                        serviceconfig.getS3().getAccessSecret());

        if(StringUtils.isNotBlank(s3.getIdToken()) && StringUtils.isBlank(s3.getAccessSecret())) {
            String[] jwtParts = s3.getIdToken().split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(jwtParts[1]));
            Map<String, Object> idToken = JsonUtils.toObject(payload, Map.class);
            if (idToken.get("sub").equals(s3.getBucket())) {
                getStsToken(s3, clientId, builder, (int) idToken.get("exp"));
            }
        } else if (StringUtils.isNotBlank(s3.getEndpoint())) {
            if (StringUtils.isNotBlank(s3.getAccessKey())) {
                clientMap.putIfAbsent(clientId, S3Client.builder()
                        .httpClientBuilder(builder)
                        .region(Region.of(s3.getRegion()))
                        .credentialsProvider(() -> AwsBasicCredentials.create(s3.getAccessKey(), s3.getAccessSecret()))
                        .endpointOverride(URI.create(s3.getEndpoint()))
                        .forcePathStyle(true)
                        .build());
            } else {
                clientMap.putIfAbsent(clientId, S3Client.builder()
                        .httpClientBuilder(builder)
                        .region(Region.of(s3.getRegion()))
                        .credentialsProvider(() -> awsCredentials)
                        .endpointOverride(URI.create(s3.getEndpoint()))
                        .forcePathStyle(true)
                        .build());
            }
        } else if (StringUtils.isNotBlank(s3.getAccessKey())) {
            clientMap.putIfAbsent(clientId, S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(() -> AwsBasicCredentials.create(s3.getAccessKey(), s3.getAccessSecret()))
                    .forcePathStyle(true)
                    .build());
        } else {
            clientMap.putIfAbsent(clientId, S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(() -> awsCredentials)
                    .forcePathStyle(true)
                    .build());
        }
    }

    private void getStsToken(S3 s3, String clientId, SdkHttpClient.Builder builder, int exp) {
        AssumeRoleWithWebIdentityRequest awRequest =
                AssumeRoleWithWebIdentityRequest.builder()
                        .durationSeconds(3600)
                        // aws need, minio optional
//                            .roleSessionName("test")
//                            .roleArn("arn:minio:bucket:us-east-1:test")
                        .webIdentityToken(s3.getIdToken())
                        .build();
        StsClient stsClient;
        if(StringUtils.isNotBlank(s3.getEndpoint())) {
            stsClient = StsClient.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(AnonymousCredentialsProvider.create())
                    .endpointOverride(URI.create(s3.getEndpoint()))
                    .build();
            Credentials credentials = stsClient.assumeRoleWithWebIdentity(awRequest).credentials();
            AwsSessionCredentials awsCredentials = AwsSessionCredentials.create(
                    credentials.accessKeyId(),
                    credentials.secretAccessKey(),
                    credentials.sessionToken());

            clientMap.put(clientId, S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(() ->  awsCredentials)
                    .endpointOverride(URI.create(s3.getEndpoint()))
                    .forcePathStyle(true)
                    .build());
        } else {
            stsClient = StsClient.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(AnonymousCredentialsProvider.create())
                    .build();
            Credentials credentials = stsClient.assumeRoleWithWebIdentity(awRequest).credentials();
            AwsSessionCredentials awsCredentials = AwsSessionCredentials.create(
                    credentials.accessKeyId(),
                    credentials.secretAccessKey(),
                    credentials.sessionToken());

            clientMap.put(clientId, S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(() ->  awsCredentials)
                    .forcePathStyle(true)
                    .build());
        }
        stsClientMap.put(clientId + "-timestamp", String.valueOf(exp));

//            StsAssumeRoleWithWebIdentityCredentialsProvider provider = StsAssumeRoleWithWebIdentityCredentialsProvider.builder()
//                    .refreshRequest(awRequest)
//                    .stsClient(stsClient)
//                    .build();
    }
}
