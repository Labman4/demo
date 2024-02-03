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
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CORSConfiguration;
import software.amazon.awssdk.services.s3.model.CORSRule;
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
import software.amazon.awssdk.services.s3.model.GetBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.GetBucketCorsResponse;
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
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutBucketCorsRequest;
import software.amazon.awssdk.services.s3.model.PutBucketCorsResponse;
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

    @Autowired
    private Map<String, S3Client> clientMap;

    private final Map<String, String> stsClientMap = new ConcurrentHashMap<>();
    @Autowired
    public ServiceConfig serviceconfig;

    @Override
    public String uploadObject(S3Client s3Client, String bucket, String key, RequestBody requestBody) {
        if (log.isDebugEnabled()) {
            log.debug("uploadObject key:{}", key);
        }
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3Client.putObject(objectRequest, requestBody).eTag();
    }

    @Override
    public void deleteObject(S3Client s3Client, String bucket, String key) {
        if (log.isDebugEnabled()) {
            log.debug("deleteObject key:{}", key);
        }
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public void deleteObjects(S3Client s3Client, String bucket, String keys) {
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
        s3Client.deleteObjects(deleteObjectRequest);
    }


    @Override
    public void deleteObjectByPrefix(S3Client s3Client, String bucket, String prefix) {
        List<ObjectIdentifier> toDelete = new ArrayList<>();
        listObject(s3Client, bucket, prefix).contents().stream().forEach(obj -> toDelete.add(ObjectIdentifier.builder()
                .key(obj.key()).build()));
        DeleteObjectsRequest deleteObjectRequest = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(toDelete).build())
                .build();
        s3Client.deleteObjects(deleteObjectRequest);
    }

    @Override
    public ResponseBytes<GetObjectResponse> getObject(S3Client s3Client, String bucket, String key) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> bytesResp = s3Client.getObjectAsBytes(objectRequest);
            if (bytesResp != null) {
                return bytesResp;
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
            return null;
        } catch (InvalidObjectStateException e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectAsBytes state error:{}", e.getMessage());
            }
            return null;
        } catch (S3Exception e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectAsBytes s3 error:{}", e.getMessage());
            }
            return null;
        }
        return null;
    }

    @Override
    public String getObjectString(S3Client s3Client, String bucket, String key) {
        try {
            ResponseBytes<GetObjectResponse> bytesResp = getObject(s3Client, bucket, key);
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
    public ResponseInputStream<GetObjectResponse> getObjectStream(S3Client s3Client, String bucket, String key, int start, int end) {
        try {
            GetObjectRequest objectRequest = null;
            GetObjectRequest.Builder builder = GetObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(key);
            if (start >= 0) {
                objectRequest = builder.range("bytes=" + start + "-").build();
                if (end > start) {
                    objectRequest = builder.range("bytes=" + start + "-" + end).build();
                }
            } else {
                objectRequest = builder.build();
            }
            ResponseInputStream<GetObjectResponse> streamResp = s3Client.getObject(objectRequest);
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
    public ListObjectsV2Iterable listObject(S3Client s3Client, String bucket, String prefix) {
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();
        if (s3Client != null) {
            return s3Client.listObjectsV2Paginator(listReq);
        } else {
            return null;
        }
    }

    @Override
    public CreateMultipartUploadResponse createMultiPart(S3Client s3Client, String bucket, String key) {
        if (log.isTraceEnabled()) {
            log.trace("create multipartUpload");
        }
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return s3Client.createMultipartUpload(createMultipartUploadRequest);
    }

    @Override
    public boolean createBucket(S3Client s3Client, String platform, String bucket) {
        try {
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            if (StringUtils.isNotBlank(platform) && "cloudflare".equals(platform)) {
                CreateBucketConfiguration createBucketConfiguration  = CreateBucketConfiguration.builder().locationConstraint("auto").build();
                bucketRequest = CreateBucketRequest.builder()
                        .bucket(bucket)
                        .createBucketConfiguration(createBucketConfiguration)
                        .build();
            }
            S3Waiter s3Waiter = s3Client.waiter();
            s3Client.createBucket(bucketRequest);
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
    public HeadObjectResponse headObject(S3Client s3Client, String bucket, String key) {
        if (log.isTraceEnabled()) {
            log.trace("headObject:{}, bucket:{}, key:{}", bucket, key);
        }
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            return s3Client.headObject(headObjectRequest);
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
    public UploadPartResponse uploadPart(S3Client s3Client, S3 s3, RequestBody requestBody, int partNum, long endOffset) throws IOException {
        UploadPartRequest uploadRequest = null;
        try {
            uploadRequest = UploadPartRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .uploadId(s3.getUploadId())
                    .partNumber(partNum)
                    .contentLength(endOffset)
                    .build();
            return s3Client.uploadPart(uploadRequest, requestBody);
        } catch (NoSuchUploadException e) {
            return null;
        } catch (SdkClientException e) {
            return s3Client.uploadPart(uploadRequest, requestBody);
        }
    }

    @Override
    public ListMultipartUploadsResponse listMultipartUploads(S3Client s3Client, String platform, String bucket) {
        ListMultipartUploadsRequest listMultipartUploadsRequest = ListMultipartUploadsRequest.builder()
                .bucket(bucket)
                .build();
        ListMultipartUploadsResponse resp = null;
        try {
            resp = s3Client.listMultipartUploads(listMultipartUploadsRequest);
        } catch (NoSuchBucketException e) {
            if (log.isWarnEnabled()) {
                log.warn("listMultipartUploads, bucket not exist, will create auto");
            }
            createBucket(s3Client, platform, bucket);
        }
        if (log.isDebugEnabled()) {
            log.debug("listMultipartUploads: {}", resp.uploads().size());
        }
        return resp;
    }

    @Override
    public void abortMultipartUpload(S3Client s3Client, String bucket, String key, String uploadId) {
        try {
            AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .uploadId(uploadId)
                            .build();
            s3Client.abortMultipartUpload(abortMultipartUploadRequest);
        } catch (NoSuchUploadException e) {
            if (log.isDebugEnabled()) {
                log.debug("upload not exist");
            }
        }
    }

    @Override
    public void listCompletedPart(S3Client s3Client, String bucket, String key, String uploadId, List<CompletedPart> completedParts) {
        ListPartsResponse listPartsResponse = null;
        try {
            listPartsResponse = listParts(s3Client, bucket, key, uploadId);
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
    public ListPartsResponse listParts(S3Client s3Client, String bucket, String key, String uploadId) {
        if (log.isDebugEnabled()) {
            log.debug("list parts uploadId:{}, bucket:{}, key:{}", uploadId, bucket, key);
        }
        try {
            ListPartsRequest listRequest = ListPartsRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .build();
            return s3Client.listParts(listRequest);
        } catch (SdkClientException e) {
            if (log.isErrorEnabled()) {
                log.error("listPart sdk client error: {}", e.getMessage());
            }
        }
        return null;
    }

    @Override
    public void completePart(S3Client s3Client, String bucket, String key, String uploadId, List<CompletedPart> completedParts) {
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
        CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(completeRequest);
        if (log.isInfoEnabled()) {
            log.info("complete MultipartUpload: {}", response.eTag());
        }
    }

    @Override
    public GetBucketCorsResponse getCorsRule(S3Client s3Client, String bucket) {
        GetBucketCorsRequest getBucketCorsRequest = GetBucketCorsRequest.builder().bucket(bucket).build();
        GetBucketCorsResponse response = null;
        try {
            response = s3Client.getBucketCors(getBucketCorsRequest);
        } catch (S3Exception e) {
            if (log.isErrorEnabled()) {
                log.error("getCorsRule error: {}", e.getMessage());
            }
        }
        return response;
    }

    @Override
    public PutBucketCorsResponse putCorsRule(S3Client s3Client, String bucket, List<CORSRule> corsRules) {
        CORSConfiguration corsConfiguration = CORSConfiguration.builder().corsRules(corsRules).build();
        PutBucketCorsRequest putBucketCorsRequest = PutBucketCorsRequest.builder().bucket(bucket).corsConfiguration(corsConfiguration).build();
        return s3Client.putBucketCors(putBucketCorsRequest);
    }
    @Override
    public S3Client initClient(S3 s3, String clientId) {
        S3Client s3Client = null;
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
        if (clientMap.containsKey(clientId) && clientMap.get(clientId) != null) {
            if (!stsClientMap.containsKey(clientId + "-timestamp")) {
                if (checkClient(s3, clientId, clientMap.get(clientId))) {
                    if (log.isTraceEnabled()) {
                        log.trace("skip init");
                    }
                    return clientMap.get(clientId);
                } else {
                    clientMap.remove(clientId);
                }
            } else {
                String timestamp = stsClientMap.get(clientId + "-timestamp");
                if (log.isDebugEnabled()) {
                    log.debug("client expired time :{}", timestamp);
                }
                if (Instant.now().compareTo(Instant.ofEpochMilli(Long.parseLong(timestamp)*1000)) < 0) {
                    if (checkClient(s3, clientId, clientMap.get(clientId))) {
                        return clientMap.get(clientId);
                    } else {
                        clientMap.remove(clientId);
                    }
                }
                if (log.isTraceEnabled()) {
                    log.trace("client expired, continue init");
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
        Long socket = serviceconfig.getTimeout().getSocket();
        Duration connectDuration = Duration.ofSeconds(connect);
        Duration socketDuration = Duration.ofSeconds(socket);
        SdkHttpClient.Builder builder = ApacheHttpClient.builder()
                .connectionTimeout(connectDuration)
                .socketTimeout(socketDuration)
                .proxyConfiguration(ProxyConfiguration.builder()
                        .useSystemPropertyValues(true)
                        .build())
                .connectionAcquisitionTimeout(connectDuration)
                .connectionMaxIdleTime(connectDuration)
                .connectionTimeToLive(connectDuration);

        if(StringUtils.isNotBlank(s3.getIdToken())
                && StringUtils.isBlank(s3.getAccessSecret())
                && StringUtils.isBlank(serviceconfig.getS3().getAccessSecret())) {
            String[] jwtParts = s3.getIdToken().split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(jwtParts[1]));
            Map<String, Object> idToken = JsonUtils.toObject(payload, Map.class);
            if (idToken.get("sub").equals(s3.getBucket())) {
                return getStsToken(s3, clientId, builder, (int) idToken.get("exp"));
            }
        } else if (StringUtils.isNotBlank(s3.getEndpoint())) {
            if (StringUtils.isNotBlank(s3.getAccessSecret())) {
                s3Client = S3Client.builder()
                        .httpClientBuilder(builder)
                        .region(Region.of(s3.getRegion()))
                        .credentialsProvider(() -> AwsBasicCredentials.create(s3.getAccessKey(), s3.getAccessSecret()))
                        .endpointOverride(URI.create(s3.getEndpoint()))
                        .forcePathStyle(true)
                        .build();
            } else {
                s3Client = S3Client.builder()
                        .httpClientBuilder(builder)
                        .region(Region.of(s3.getRegion()))
                        .credentialsProvider(() -> AwsBasicCredentials.create(
                                        serviceconfig.getS3().getAccessKey(),
                                        serviceconfig.getS3().getAccessSecret()))
                        .endpointOverride(URI.create(s3.getEndpoint()))
                        .forcePathStyle(true)
                        .build();
            }
        } else if (StringUtils.isNotBlank(s3.getAccessSecret())) {
            s3Client = S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(() -> AwsBasicCredentials.create(s3.getAccessKey(), s3.getAccessSecret()))
                    .forcePathStyle(true)
                    .build();
        } else {
            s3Client = S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(() -> AwsBasicCredentials.create(
                            serviceconfig.getS3().getAccessKey(),
                            serviceconfig.getS3().getAccessSecret()))
                    .forcePathStyle(true)
                    .build();
        }
        if (checkClient(s3, clientId, s3Client)) {
            return s3Client;
        } else {
            return null;
        }
    }

    private boolean checkClient(S3 s3, String clientId, S3Client s3Client) {
        if (log.isDebugEnabled()) {
            log.debug("checkClient clientId:{}, s3Client:{}", clientId, s3Client);
        }
        try {
            listMultipartUploads(s3Client, s3.getPlatform(), s3.getBucket());
        } catch (Exception e) {
            return false;
        }
        clientMap.putIfAbsent(clientId, s3Client);
        return true;
    }

    private S3Client getStsToken(S3 s3, String clientId, SdkHttpClient.Builder builder, int exp) {
        AssumeRoleWithWebIdentityRequest awRequest =
                AssumeRoleWithWebIdentityRequest.builder()
                        .durationSeconds(3600)
                        // aws need, minio optional
//                            .roleSessionName("test")
//                            .roleArn("arn:minio:bucket:us-east-1:test")
                        .webIdentityToken(s3.getIdToken())
                        .build();
        StsClient stsClient;
        S3Client s3Client = null;
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
            s3Client =  S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(() ->  awsCredentials)
                    .endpointOverride(URI.create(s3.getEndpoint()))
                    .forcePathStyle(true)
                    .build();
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
            s3Client = S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(() ->  awsCredentials)
                    .forcePathStyle(true)
                    .build();
        }
        if(checkClient(s3, clientId, s3Client)) {
            stsClientMap.put(clientId + "-timestamp", String.valueOf(exp));
            return s3Client;
        } else {
            return null;
        }

//            StsAssumeRoleWithWebIdentityCredentialsProvider provider = StsAssumeRoleWithWebIdentityCredentialsProvider.builder()
//                    .refreshRequest(awRequest)
//                    .stsClient(stsClient)
//                    .build();
    }
}
