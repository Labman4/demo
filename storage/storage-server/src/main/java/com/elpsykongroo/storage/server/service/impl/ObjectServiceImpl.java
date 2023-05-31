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

package com.elpsykongroo.storage.server.service.impl;

import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.storage.server.entity.S3;
import com.elpsykongroo.storage.server.service.ObjectService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.ProxyConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.services.sts.StsClient;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ObjectServiceImpl implements ObjectService {
    @Autowired
    private ServiceConfig serviceconfig;

    private S3Client s3Client;

    private Long partSize = (long) 1024 * 1024 * 5;

    @Override
    public void download(S3 s3, HttpServletResponse response) throws IOException {
        initClient(s3);
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .key(s3.getKey())
                .bucket(s3.getBucket())
                .build();
        if (StringUtils.isNotBlank(s3.getOffset())) {
            long startPoint = Long.parseLong(s3.getOffset()); // 断点续传的起始位置
            objectRequest.toBuilder().range("bytes=" + startPoint + "-");
        }
        ResponseInputStream<GetObjectResponse> in = s3Client.getObject(objectRequest);
        response.setHeader("Content-Type", in.response().contentType());
        response.setHeader("Content-Disposition", "attachment; filename=" + s3.getKey());
        BufferedInputStream inputStream = new BufferedInputStream(in);
        OutputStream out = null;
        try {
            out = response.getOutputStream();
            byte[] b = new byte[1024];
            int len ;
            while ((len = inputStream.read(b)) != -1) {
                out.write(b,0,len);
            }
        } finally {
            out.flush();
            out.close();
        }
    }

    private void upload(S3 s3) throws IOException {
        log.debug("upload");
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .build();
        s3Client.putObject(objectRequest, RequestBody.fromBytes(s3.getData()[0].getBytes()));
        log.debug("upload complete");

    }

    @Override
    public void delete(S3 s3) {
        initClient(s3);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3.getBucket())
                .key(s3.getKey())
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public Map<String, Long> list(S3 s3) {
        initClient(s3);
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(s3.getBucket())
                .maxKeys(1)
                .build();

        ListObjectsV2Iterable listRes = s3Client.listObjectsV2Paginator(listReq);

        Map<String, Long> result = new HashMap<>();
        listRes.contents().stream()
                .forEach(content -> result.put(content.key(), content.size()));

        return result;
    }

    @Override
    public void multipartUpload(S3 s3) throws Exception {
        initClient(s3);
        if (StringUtils.isBlank(s3.getKey())) {
            s3.setKey(s3.getData()[0].getOriginalFilename());
        }
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(s3.getBucket())
                .key(s3.getKey())
                .build();

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createMultipartUploadRequest);
        String uploadId = response.uploadId();

        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
            .bucket(s3.getBucket())
            .key(s3.getKey())
            .build();

        try {
            HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
            Long length = headObjectResponse.getValueForField("ContentLength", Long.class).get();
            log.debug("exist object length:{}", length);
            if (length > 0) {
                continueUpload(s3, uploadId);
            }
        } catch (NoSuchKeyException e) {
            uploadPart(s3, uploadId);
        }
    }

    private void uploadPart(S3 s3, String uploadId) throws IOException {
        long fileSize = s3.getData()[0].getSize();
        partSize = Math.max(Long.parseLong(s3.getPartSize()), 5 * 1024 * 1024); // 最小为 5MB
        if (fileSize < partSize) {
            upload(s3);
            return;
        }
        log.debug("uploadPart");
        int num = (int) Math.ceil((double) s3.getData()[0].getSize() / partSize);
        List<CompletedPart> completedParts = new ArrayList<CompletedPart>();
            for(int i = 0; i< num ; i++) {
                int percent = (int) Math.ceil((double) (i + 1) / num * 100);
                log.debug("uploadPart complete:{} ", percent + "%");
                long startOffset = i * partSize;
                long endOffset = Math.min(partSize, fileSize - startOffset);
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(s3.getBucket())
                        .key(s3.getKey())
                        .uploadId(uploadId)
                        .partNumber(i + 1)
                        .contentLength(endOffset)
                        .build();
                UploadPartResponse uploadPartResponse =
                        s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(s3.getData()[0].getBytes()));
                completedParts.add(
                        CompletedPart.builder()
                                .partNumber(i + 1)
                                .eTag(uploadPartResponse.eTag())
                                .build()
                );
            }
                CompleteMultipartUploadRequest completeMultipartUploadRequest =
                        CompleteMultipartUploadRequest.builder()
                                .bucket(s3.getBucket())
                                .key(s3.getKey())
                                .uploadId(uploadId)
                                .multipartUpload(CompletedMultipartUpload
                                        .builder()
                                        .parts(completedParts)
                                        .build())
                                .build();
                s3Client.completeMultipartUpload(completeMultipartUploadRequest);
            log.debug("uploadPart complete");
    }

    private void continueUpload(S3 s3, String uploadId) throws Exception {
        long fileSize = s3.getData()[0].getSize();
        partSize = Math.max(Long.parseLong(s3.getPartSize()), 5 * 1024 * 1024); // 最小为 5MB
        if (fileSize < partSize) {
            upload(s3);
            return;
        }
        log.debug("continue to upload");
        List<CompletedPart> completedParts = new ArrayList<CompletedPart>();

        ListPartsRequest listRequest = ListPartsRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .uploadId(uploadId)
                    .build();

        ListPartsResponse listResponse = s3Client.listParts(listRequest);
            if (listResponse.parts().size() > 0) {
                uploadId = listResponse.uploadId();
                listResponse.parts().stream().map(o ->
                        completedParts.add(CompletedPart.builder()
                                .partNumber(o.getValueForField("PartNumber", Integer.class).get())
                                .eTag(o.getValueForField("ETag", String.class).get())
                                .build())

                );
            }

            // 计算总分片数
            int partCount = (int) Math.ceil((double) fileSize / partSize);

            for (int i = completedParts.size(); i < partCount; i++) {
                int percent = (int) Math.ceil((double) (i + 1) / partCount * 100);
                log.debug("uploadPart complete:{} ", percent + "%");
                long startPos = i * partSize;
                long partLength = Math.min(partSize, fileSize - startPos);
                UploadPartRequest uploadRequest = UploadPartRequest.builder()
                        .bucket(s3.getBucket())
                        .key(s3.getKey())
                        .uploadId(uploadId)
                        .partNumber(i + 1)
                        .contentLength(partLength)
                        .build();

                // 从文件的指定位置开始上传分片
                    InputStream in = new ByteArrayInputStream(s3.getData()[0].getBytes());
                    s3Client.uploadPart(uploadRequest, RequestBody.fromInputStream(in, partLength));
            }

            // 如果已上传的分片数等于总分片数，表示上传完成，可以合并分片
            if (completedParts.size() == partCount) {
                CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                        .bucket(s3.getBucket())
                        .key(s3.getKey())
                        .uploadId(uploadId)
                        .multipartUpload(
                                CompletedMultipartUpload.builder()
                                        .parts(completedParts)
                                        .build()
                        )
                        .build();
                log.debug("continue to upload complete");
            }
        log.debug("continue to upload complete");
    }

    private void initClient(S3 s3) {
        String accessKey = serviceconfig.getS3().getAccessKey();
        String accessSecret = serviceconfig.getS3().getAccessSecret();
        String endpoint = serviceconfig.getS3().getEndpoint();
        String region = serviceconfig.getS3().getRegion();
        String proxyUrl = serviceconfig.getProxy();
        AwsCredentials awsCredentials = AwsBasicCredentials.create(accessKey, accessSecret);
        if (StringUtils.isNotBlank(s3.getRegion())) {
            region = s3.getRegion();
        }

        if (StringUtils.isNotBlank(s3.getEndpoint())) {
            endpoint = s3.getEndpoint();
        }

        if(StringUtils.isNotBlank(s3.getIdToken())) {
            getStsToken(s3, region, proxyUrl);
        } else if (StringUtils.isNotBlank(endpoint)) {
            if (StringUtils.isNotBlank(s3.getAccessKey())) {
                this.s3Client = S3Client.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder()
                                .proxyConfiguration(ProxyConfiguration.builder()
                                .endpoint(URI.create(proxyUrl))
                                .build()))
                        .region(Region.of(region))
                        .credentialsProvider(() -> AwsBasicCredentials.create(s3.getAccessKey(), s3.getAccessSecret()))
                        .endpointOverride(URI.create(endpoint))
                        .forcePathStyle(true)
                        .build();
            } else {
                this.s3Client = S3Client.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder()
                                .proxyConfiguration(ProxyConfiguration.builder()
                                        .endpoint(URI.create(proxyUrl))
                                        .build()))
                        .region(Region.of(region))
                        .credentialsProvider(() -> awsCredentials)
                        .endpointOverride(URI.create(endpoint))
                        .forcePathStyle(true)
                        .build();
            }
        } else if (StringUtils.isNotBlank(s3.getAccessKey())) {
                this.s3Client = S3Client.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder()
                                .proxyConfiguration(ProxyConfiguration.builder()
                                        .endpoint(URI.create(proxyUrl))
                                        .build()))
                        .region(Region.of(region))
                        .credentialsProvider(() -> AwsBasicCredentials.create(s3.getAccessKey(), s3.getAccessSecret()))
                        .forcePathStyle(true)
                        .build();
        } else {
            this.s3Client = S3Client.builder()
                    .httpClientBuilder(UrlConnectionHttpClient.builder()
                            .proxyConfiguration(ProxyConfiguration.builder()
                                    .endpoint(URI.create(proxyUrl))
                                    .build()))
                    .region(Region.of(region))
                    .credentialsProvider(() -> awsCredentials)
                    .forcePathStyle(true)
                    .build();
        }
    }

    void getStsToken(S3 s3, String region, String proxyUrl) {
            AssumeRoleWithWebIdentityRequest awRequest =
                    AssumeRoleWithWebIdentityRequest.builder()
                            .durationSeconds(3600)
                            .webIdentityToken(s3.getIdToken())
                            .build();

            StsClient stsClient = null;
            if(StringUtils.isNotBlank(s3.getEndpoint())) {
                stsClient = StsClient.builder()
                        .region(Region.of(s3.getRegion()))
                        .credentialsProvider(AnonymousCredentialsProvider.create())
                        .endpointOverride(URI.create(s3.getEndpoint()))
                        .build();
                Credentials credentials = stsClient.assumeRoleWithWebIdentity(awRequest).credentials();
                AwsSessionCredentials awsCredentials = AwsSessionCredentials.create(
                        credentials.accessKeyId(),
                        credentials.secretAccessKey(),
                        credentials.sessionToken());

                this.s3Client = S3Client.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder()
                                .proxyConfiguration(ProxyConfiguration.builder()
                                        .endpoint(URI.create(proxyUrl))
                                        .build()))
                        .region(Region.of(region))
                        .credentialsProvider(() ->  awsCredentials)
                        .endpointOverride(URI.create(s3.getEndpoint()))
                        .forcePathStyle(true)
                        .build();
            } else {
                stsClient = StsClient.builder()
                        .region(Region.of(s3.getRegion()))
                        .credentialsProvider(AnonymousCredentialsProvider.create())
                        .build();
                Credentials credentials = stsClient.assumeRoleWithWebIdentity(awRequest).credentials();
                AwsSessionCredentials awsCredentials = AwsSessionCredentials.create(
                        credentials.accessKeyId(),
                        credentials.secretAccessKey(),
                        credentials.sessionToken());

                this.s3Client = S3Client.builder()
                        .httpClientBuilder(UrlConnectionHttpClient.builder()
                                .proxyConfiguration(ProxyConfiguration.builder()
                                        .endpoint(URI.create(proxyUrl))
                                        .build()))
                        .region(Region.of(region))
                        .credentialsProvider(() ->  awsCredentials)
                        .forcePathStyle(true)
                        .build();
            }
//            StsAssumeRoleWithWebIdentityCredentialsProvider provider = StsAssumeRoleWithWebIdentityCredentialsProvider.builder()
//                    .refreshRequest(awRequest)
//                    .stsClient(stsClient)
//                    .build();
        }
}
