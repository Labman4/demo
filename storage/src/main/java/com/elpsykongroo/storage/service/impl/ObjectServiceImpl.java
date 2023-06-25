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
import com.elpsykongroo.base.domain.storage.object.ListObject;
import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.storage.listener.ObjectListener;
import com.elpsykongroo.storage.service.ObjectService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
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
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsResponse;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.NoSuchUploadException;
import software.amazon.awssdk.services.s3.model.Part;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.sts.model.AssumeRoleWithWebIdentityRequest;
import software.amazon.awssdk.services.sts.model.Credentials;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.services.sts.StsClient;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class ObjectServiceImpl implements ObjectService {

    private S3Client s3Client;

    private Long partSize = (long) 1024 * 1024 * 5;

    @Autowired
    private KafkaAdmin kafkaAdmin;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ServiceConfig serviceconfig;

    @Autowired
    private ApplicationContext ac;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @Autowired
    private KafkaListenerEndpointRegistry endpointRegistry;

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
        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(response.getOutputStream());
            byte[] b = new byte[1024];
            int len ;
            while ((len = inputStream.read(b)) != -1) {
                out.write(b, 0, len);
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
        s3Client.putObject(objectRequest, RequestBody.fromBytes(s3.getData()[0].getBytes())).eTag();
        log.debug("upload complete");
    }

    @Override
    public void delete(S3 s3) {
        initClient(s3);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3.getBucket())
                .key(s3.getKey())
                .build();
        s3Client.deleteObject(deleteObjectRequest).deleteMarker();
    }

    @Override
    public List<ListObject> list(S3 s3) {
        initClient(s3);
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(s3.getBucket())
                .maxKeys(1)
                .build();
        List<ListObject> objects = new ArrayList<>();
        ListObjectsV2Iterable listRes = null;
        try {
            listObject(listReq, objects);
        } catch (NoSuchBucketException e) {
            log.info("bucket not exist");
            if(createBucket(s3)) {
                listObject(listReq, objects);
            }
        }
        return objects;
    }

    private void listObject(ListObjectsV2Request listReq, List<ListObject> objects) {
        ListObjectsV2Iterable listRes;
        listRes = s3Client.listObjectsV2Paginator(listReq);
        listRes.contents().stream()
                .forEach(content -> objects.add(new ListObject(content.key(),
                        content.lastModified(),
                        content.size())));
    }

    @Override
    public void multipartUpload(S3 s3) throws Exception {
        initClient(s3);
        if (StringUtils.isBlank(s3.getKey())) {
            s3.setKey(StringUtils.trim(s3.getData()[0].getOriginalFilename()));
        }
        HeadObjectResponse headObjectResponse = headObject(s3);
        if (headObjectResponse != null) {
            log.debug("object exist skip upload");
            return;
        }
        if (StringUtils.isNotBlank(s3.getUploadId())) {
            uploadPart(s3, false);
        } else {
            boolean flag = false;
            List<MultipartUpload> uploads = listMultipartUploads(s3).uploads();
            log.debug("multipartUpload size:{}", uploads.size());
            for (MultipartUpload upload: uploads) {
                if (s3.getKey().equals(upload.key())) {
                    flag = true;
                    s3.setUploadId(upload.uploadId());
                    uploadPart(s3, true);
                }
            }
            if (!flag) {
                CreateMultipartUploadResponse resp = createMultiPart(s3);
                s3.setUploadId(resp.uploadId());
                uploadPart(s3,false);
            }
//            AbortMultipartUploadRequest abortMultipartUploadRequest;
//            for (MultipartUpload upload: uploads) {
//                abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
//                        .bucket(s3.getBucket())
//                        .key(upload.key())
//                        .uploadId(upload.uploadId())
//                        .build();
//
//                s3Client.abortMultipartUpload(abortMultipartUploadRequest);
//            }
        }
    }

    public boolean createBucket(S3 s3) {
        try {
            initClient(s3);
            S3Waiter s3Waiter = s3Client.waiter();
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(s3.getBucket())
                    .build();

            s3Client.createBucket(bucketRequest);
            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(s3.getBucket())
                    .build();

            WaiterResponse<HeadBucketResponse> waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
            return waiterResponse.matched().response().isPresent();
        } catch (S3Exception e) {
            log.error("create bucket error: {}", e.awsErrorDetails().errorMessage());
        }
        return false;
    }


    private void uploadStream(S3 s3, Integer num, String uploadId) {
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        adminClient.createTopics(Collections.singleton(TopicBuilder.name(s3.getKey()).build()));
        Consumer<String, String> consumer = consumerFactory.createConsumer(s3.getBucket());
        TopicDescription topicDescription = null;
        try {
            topicDescription = adminClient.describeTopics(Collections.singleton(s3.getKey()))
                    .topicNameValues().get(s3.getKey()).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        long topicSize = 0;
        for (TopicPartitionInfo partitionInfo : topicDescription.partitions()) {
            TopicPartition topicPartition = new TopicPartition(s3.getKey(), partitionInfo.partition());
            long partitionSize = (long) consumer.endOffsets(Collections.singleton(topicPartition)).get(topicPartition);
            topicSize += partitionSize;
        }
        byte[][] output = new byte[num][];
        long timestamp = Instant.now().toEpochMilli();
        if (topicSize != num) {
            log.debug("uploadStream");
            for(int i = 0; i< num ; i++) {
                int percent = (int) Math.ceil((double) i / num * 100);
                log.debug("uploadStream complete:{} ", percent + "%");
                long startOffset = i * partSize;
                try {
                    output[i] = Arrays.copyOfRange(s3.getData()[0].getBytes(), (int) startOffset, (int) (startOffset + partSize));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                kafkaTemplate.send(s3.getKey(),
                        s3.getBucket() + "-" + timestamp + "*" + s3.getKey() + "*" + num + "*" + i + "*" + uploadId, output[i]);
            }
        }
        ac.getBean(ObjectListener.class, s3.getBucket() + "-" + timestamp, s3.getKey(), this);
    }

    private void uploadPart(S3 s3, Boolean resume) throws IOException {
        partSize = Math.max(Long.parseLong(s3.getPartSize()), 5 * 1024 * 1024);
        RequestBody requestBody = null;
        long fileSize = 0;
        int num = 0;
        if (s3.getByteData() != null) {
            requestBody = RequestBody.fromBytes(s3.getByteData());
            fileSize = s3.getByteData().length;
            num = (int) Math.ceil((double) fileSize / partSize);
        } else {
            requestBody = RequestBody.fromBytes(s3.getData()[0].getBytes());
            fileSize = s3.getData()[0].getSize();
            num = (int) Math.ceil((double) fileSize / partSize);
            if ("stream".equals(s3.getMode()) && fileSize >= partSize) {
                uploadStream(s3, num, s3.getUploadId());
            }
        }
        if (fileSize < partSize) {
            upload(s3);
            return;
        }
        if(!"stream".equals(s3.getMode())) {
            log.debug("uploadPart, continue:{}", resume);
            List<CompletedPart> completedParts = new ArrayList<CompletedPart>();
            int startPart = 0;
            if (resume) {
                listCompletedPart(s3, completedParts);
                startPart = completedParts.size();
            }
            for(int i = startPart; i < num ; i++) {
                int percent = (int) Math.ceil((double) i / num * 100);
                log.debug("uploadPart complete:{} ", percent + "%");
                long startOffset = i * partSize;
                long endOffset = Math.min(partSize, fileSize - startOffset);
                int partNum = i + 1;
                if (StringUtils.isNotBlank(s3.getPartNum())) {
                    partNum = Integer.parseInt(s3.getPartNum()) + 1;
                }
                UploadPartResponse uploadPartResponse = uploadPart(s3, requestBody, partNum, endOffset);
                if (uploadPartResponse != null) {
                    completedParts.add(
                            CompletedPart.builder()
                                    .partNumber(partNum)
                                    .eTag(uploadPartResponse.eTag())
                                    .build()
                    );
                }
            }
            if (StringUtils.isNotBlank(s3.getPartCount())) {
                completedParts = new ArrayList<CompletedPart>();
                listCompletedPart(s3, completedParts);
                if (completedParts.size() == Integer.parseInt(s3.getPartCount())) {
                    completePart(s3, completedParts);
                    completeTopic(s3);
                }
            } else if (completedParts.size() == num) {
                completePart(s3, completedParts);
            }
        }
    }

    private void completeTopic(S3 s3) {
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        MessageListenerContainer container = endpointRegistry.getListenerContainer(s3.getConsumerId());
        if (container != null && container.isRunning()) {
           container.stop();
        }
        while (!container.isRunning()) {
            adminClient.deleteTopics(Collections.singleton(s3.getKey()));
        }
    }

    private HeadObjectResponse headObject(S3 s3) {
        log.debug("headObject");
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .build();
            return s3Client.headObject(headObjectRequest);
        } catch (NoSuchKeyException e) {
            log.info("object not exist");
            return null;
        } catch (S3Exception e) {
            log.error("headObject error:{}", e.getMessage());
            return null;
        }
    }

    private CreateMultipartUploadResponse createMultiPart(S3 s3) {
        log.debug("create multipartUpload");
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(s3.getBucket())
                .key(s3.getKey())
                .build();

        return s3Client.createMultipartUpload(createMultipartUploadRequest);
    }


    private UploadPartResponse uploadPart(S3 s3, RequestBody requestBody, int partNum, long endOffset) {
        log.debug("uploadPart");
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
            s3.setUploadId(createMultiPart(s3).uploadId());
            uploadRequest = UploadPartRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .uploadId(s3.getUploadId())
                    .partNumber(partNum)
                    .contentLength(endOffset)
                    .build();
            return s3Client.uploadPart(uploadRequest, requestBody);
        } catch (SdkClientException e) {
            throw new RuntimeException(e);
        }
    }

    private ListMultipartUploadsResponse listMultipartUploads(S3 s3) {
        log.debug("listMultipartUploads");
        ListMultipartUploadsRequest listMultipartUploadsRequest = ListMultipartUploadsRequest.builder()
                .bucket(s3.getBucket())
                .build();
        return s3Client.listMultipartUploads(listMultipartUploadsRequest);
    }

    private void listCompletedPart(S3 s3, List<CompletedPart> completedParts) {
        log.debug("listCompletedPart");
        ListPartsResponse listPartsResponse = listParts(s3);
        if (listPartsResponse.parts().size() > 0) {
            for (Part part: listPartsResponse.parts()) {
                completedParts.add(CompletedPart.builder()
                        .partNumber(part.getValueForField("PartNumber", Integer.class).get())
                        .eTag(part.getValueForField("ETag", String.class).get())
                        .build());
            }
        }
    }

    private ListPartsResponse listParts(S3 s3) {
        log.debug("listParts");
        ListPartsRequest listRequest = ListPartsRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .uploadId(s3.getUploadId())
                    .build();
        return s3Client.listParts(listRequest);
    }

    private void completePart(S3 s3, List<CompletedPart> completedParts) {
        log.debug("completePart");
        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                .bucket(s3.getBucket())
                .key(s3.getKey())
                .uploadId(s3.getUploadId())
                .multipartUpload(
                        CompletedMultipartUpload.builder()
                                .parts(completedParts)
                                .build()
                )
                .build();
        s3Client.completeMultipartUpload(completeRequest);
        log.debug("complete MultipartUpload");
    }


    private void initClient(S3 s3) {
        if (s3Client != null) {
            return;
        }
        AwsCredentials awsCredentials =
                AwsBasicCredentials.create(
                        serviceconfig.getS3().getAccessKey(),
                        serviceconfig.getS3().getAccessSecret());
        if (StringUtils.isBlank(s3.getRegion())) {
            s3.setRegion(serviceconfig.getS3().getRegion());
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
//        SdkHttpClient.Builder builder = UrlConnectionHttpClient.builder()
//                .proxyConfiguration(ProxyConfiguration.builder()
//                        .useSystemPropertyValues(true)
//                        .build())
//                .connectionTimeout(Duration.ofSeconds(serviceconfig.getTimeout().getConnect()))
//                .socketTimeout(Duration.ofSeconds(serviceconfig.getTimeout().getSocket()));
        if(StringUtils.isNotBlank(s3.getIdToken())) {
            getStsToken(s3, builder);
        } else if (StringUtils.isNotBlank(s3.getEndpoint())) {
            if (StringUtils.isNotBlank(s3.getAccessKey())) {
                this.s3Client = S3Client.builder()
                        .httpClientBuilder(builder)
                        .region(Region.of(s3.getRegion()))
                        .credentialsProvider(() -> AwsBasicCredentials.create(s3.getAccessKey(), s3.getAccessSecret()))
                        .endpointOverride(URI.create(s3.getEndpoint()))
                        .forcePathStyle(true)
                        .build();
            } else {
                this.s3Client = S3Client.builder()
                        .httpClientBuilder(builder)
                        .region(Region.of(s3.getRegion()))
                        .credentialsProvider(() -> awsCredentials)
                        .endpointOverride(URI.create(s3.getEndpoint()))
                        .forcePathStyle(true)
                        .build();
            }
        } else if (StringUtils.isNotBlank(s3.getAccessKey())) {
                this.s3Client = S3Client.builder()
                        .httpClientBuilder(builder)
                        .region(Region.of(s3.getRegion()))
                        .credentialsProvider(() -> AwsBasicCredentials.create(s3.getAccessKey(), s3.getAccessSecret()))
                        .forcePathStyle(true)
                        .build();
        } else {
            this.s3Client = S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(() -> awsCredentials)
                    .forcePathStyle(true)
                    .build();
        }
    }

    private void getStsToken(S3 s3, SdkHttpClient.Builder builder) {
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

            this.s3Client = S3Client.builder()
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

            this.s3Client = S3Client.builder()
                    .httpClientBuilder(builder)
                    .region(Region.of(s3.getRegion()))
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
