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
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.storage.listener.ObjectListener;
import com.elpsykongroo.storage.service.ObjectService;
import java.util.Base64;
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
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
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
import java.util.Map;

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
            inputStream.close();
        }

    }

    private void upload(S3 s3) throws IOException {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .build();
        s3Client.putObject(objectRequest, RequestBody.fromBytes(s3.getData()[0].getBytes())).eTag();
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
        try {
            listObject(listReq, objects);
        } catch (NoSuchBucketException e) {
            if (log.isWarnEnabled()) {
                log.warn("bucket not exist");
            }
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
    public void multipartUpload(S3 s3) throws IOException {
        initClient(s3);
        if (StringUtils.isBlank(s3.getKey())) {
            s3.setKey(StringUtils.trim(s3.getData()[0].getOriginalFilename()));
        }
        HeadObjectResponse headObjectResponse = headObject(s3);
        if (headObjectResponse != null) {
            if (log.isWarnEnabled()) {
                log.warn("object exist skip upload");
            }
            return;
        }
        if (StringUtils.isBlank(s3.getUploadId())) {
            s3.setUploadId(getUploadId(s3));
        }
        uploadPart(s3);
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

    @Override
    public String getUploadId(S3 s3) throws IOException {
        if (!"minio".equals(s3.getPlatform())) {
            List<MultipartUpload> uploads = listMultipartUploads(s3).uploads();
            if (log.isInfoEnabled()) {
                log.info("multipartUpload size:{}", uploads.size());
            }
            for (MultipartUpload upload : uploads) {
                if (s3.getKey().equals(upload.key())) {
                    return upload.uploadId();
                }
            }
        }
        return createMultiPart(s3).uploadId();
    }

    private CreateMultipartUploadResponse createMultiPart(S3 s3) {
        if (log.isDebugEnabled()) {
            log.debug("create multipartUpload");
        }
        initClient(s3);
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(s3.getBucket())
                .key(s3.getKey())
                .build();
        return s3Client.createMultipartUpload(createMultipartUploadRequest);
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
            if (log.isErrorEnabled()) {
                log.error("create bucket error: {}", e.awsErrorDetails().errorMessage());
            }
        }
        return false;
    }

    private void uploadStream(S3 s3, Integer num, String uploadId) throws IOException {
        long timestamp = Instant.now().toEpochMilli();
        long topicSize = 0;
        int start = 0;
        int partCount = num;
        if (StringUtils.isNotBlank(s3.getPartCount())) {
            partCount = Integer.parseInt(s3.getPartCount());
        }
        String topic = s3.getPlatform() + "-" +s3.getBucket() + "-" + s3.getKey();
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        adminClient.createTopics(Collections.singleton(TopicBuilder.name(topic).build()));
        ac.getBean(ObjectListener.class, s3.getBucket() + "-" + timestamp, topic, this);
        Consumer<String, String> consumer = consumerFactory.createConsumer(s3.getBucket());
        TopicDescription topicDescription = null;
        try {
            topicDescription = adminClient.describeTopics(Collections.singleton(topic))
                    .topicNameValues().get(topic).get();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("describe topic error:{}", e.getMessage());
            }
        }
        for (TopicPartitionInfo partitionInfo : topicDescription.partitions()) {
            TopicPartition topicPartition = new TopicPartition(topic, partitionInfo.partition());
            long partitionSize = (long) consumer.endOffsets(Collections.singleton(topicPartition)).get(topicPartition);
            topicSize += partitionSize;
        }
        byte[][] output = new byte[num][];
        if (uploadId.equals(getUploadId(s3))) {
            if (topicSize != partCount) {
                /**
                 *   because of upload part by asc,
                 *   continue upload start from last part,
                 *   and retry the last part
                 *   need to query uploadId first, so not compatible with minio which not support listMultipartUploads
                 */
                if (topicSize < partCount && topicSize > 0) {
                    start = (int) topicSize - 1;
                }
                // upload with js chunk upload
                if (topicSize > num) {
                    start = num - 1;
                }
                uploadPartByStream(s3, num, uploadId, timestamp, start, partCount, topic, output);
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("all part have been upload complete, skip upload part by stream");
                }
            }
        } else {
            uploadPartByStream(s3, num, uploadId, timestamp, start, partCount, topic, output);
        }
    }

    private void uploadPartByStream(S3 s3, Integer num, String uploadId, long timestamp, int start, int partCount, String topic, byte[][] output) throws IOException {
        for(int i = start; i < num; i++) {
            int percent = (int) Math.ceil((double) i / num * 100);
            if (log.isInfoEnabled()) {
                log.info("uploadStream complete:{} ", percent + "%");
            }
            int partNum = i;
            if (StringUtils.isNotBlank(s3.getPartNum())) {
                partNum = Integer.parseInt(s3.getPartNum());
            }
            long startOffset = i * partSize;
            long endOffset = startOffset + Math.min(partSize, s3.getData()[0].getSize() - startOffset);
            output[i] = Arrays.copyOfRange(s3.getData()[0].getBytes(), (int) startOffset, (int) endOffset);
            if (log.isDebugEnabled()) {
                log.debug("uploadStream part {}-{} ", partCount, partNum);
            }
            kafkaTemplate.send(topic,
                    s3.getPlatform() + "*" + s3.getBucket() + "-" + timestamp + "*" + s3.getKey() + "*" + partCount + "*" + partNum + "*" + uploadId, output[i]);
        }
    }

    private void uploadPart(S3 s3) throws IOException {
        partSize = Math.max(Long.parseLong(s3.getPartSize()), 5 * 1024 * 1024);
        RequestBody requestBody = null;
        long fileSize = 0;
        int num = 1;
        if (s3.getByteData() != null) {
            requestBody = RequestBody.fromBytes(s3.getByteData());
            fileSize = s3.getByteData().length;
            num = (int) Math.ceil((double) fileSize / partSize);
        } else {
            requestBody = RequestBody.fromBytes(s3.getData()[0].getBytes());
            fileSize = s3.getData()[0].getSize();
            num = (int) Math.ceil((double) fileSize / partSize);
            if ("stream".equals(s3.getMode()) && (fileSize >= partSize || StringUtils.isNotBlank(s3.getPartNum()))) {
                uploadStream(s3, num, s3.getUploadId());
            }
        }
        if (fileSize < partSize && StringUtils.isEmpty(s3.getPartNum())) {
            upload(s3);
            return;
        }
        if(!"stream".equals(s3.getMode())) {
            List<CompletedPart> completedParts = new ArrayList<CompletedPart>();
            int startPart = 0;
            listCompletedPart(s3, completedParts);
            if (completedParts.size() > 0 && completedParts.size() < num) {
                startPart = completedParts.size();
            }
            for(int i = startPart; i < num ; i++) {
                int percent = (int) Math.ceil((double) i / num * 100);
                long startOffset = i * partSize;
                long endOffset = Math.min(partSize, fileSize - startOffset);
                int partNum = i + 1;
                if (StringUtils.isNotBlank(s3.getPartNum())) {
                    partNum = Integer.parseInt(s3.getPartNum()) + 1;
                }
                if (log.isInfoEnabled()) {
                    log.info("uploadPart part:{}, complete:{}", partNum, percent + "%");
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
                    if (StringUtils.isNotBlank(s3.getConsumerId())) {
                        completeTopic(s3);
                    }
                }
            } else if (completedParts.size() == num) {
                completePart(s3, completedParts);
            }
        }
    }

    private void completeTopic(S3 s3) {
        if (log.isDebugEnabled()) {
            log.debug("completeTopic");
        }
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        MessageListenerContainer container = endpointRegistry.getListenerContainer(s3.getConsumerId());
        if (container != null) {
            if (container.isRunning()) {
                container.stop();
            }
            while (!container.isRunning()) {
                String topic = s3.getPlatform() + "-" + s3.getBucket() + "-" + s3.getKey();
                if (log.isDebugEnabled()) {
                    log.debug("deleteTopic :{}", topic);
                }
                TopicDescription topicDescription = null;
                try {
                    topicDescription = adminClient.describeTopics(Collections.singleton(topic))
                            .topicNameValues().get(topic).get();
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("describe topic error:{}", e.getMessage());
                    }
                }
                if (topicDescription != null) {
                    adminClient.deleteTopics(Collections.singleton(topic));
                    return;
                }
            }
        }
    }

    private HeadObjectResponse headObject(S3 s3) {
        if (log.isDebugEnabled()) {
            log.debug("headObject");
        }
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .build();
            return s3Client.headObject(headObjectRequest);
        } catch (NoSuchKeyException e) {
            if (log.isWarnEnabled()) {
                log.warn("object not exist");
            }
            return null;
        } catch (S3Exception e) {
            if (log.isErrorEnabled()) {
                log.error("headObject error:{}", e.getMessage());
            }
            return null;
        }
    }

    private UploadPartResponse uploadPart(S3 s3, RequestBody requestBody, int partNum, long endOffset) {
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
            return s3Client.uploadPart(uploadRequest, requestBody);
        }
    }

    private ListMultipartUploadsResponse listMultipartUploads(S3 s3) {
        initClient(s3);
        ListMultipartUploadsRequest listMultipartUploadsRequest = ListMultipartUploadsRequest.builder()
                .bucket(s3.getBucket())
                .build();
        ListMultipartUploadsResponse resp = s3Client.listMultipartUploads(listMultipartUploadsRequest);
        if (log.isDebugEnabled()) {
            log.debug("listMultipartUploads: {}", resp.uploads().size());
        }
        return resp;
    }

    private void listCompletedPart(S3 s3, List<CompletedPart> completedParts) {
        ListPartsResponse listPartsResponse = listParts(s3);
        if (listPartsResponse.parts().size() > 0) {
            for (Part part: listPartsResponse.parts()) {
                completedParts.add(CompletedPart.builder()
                        .partNumber(part.getValueForField("PartNumber", Integer.class).get())
                        .eTag(part.getValueForField("ETag", String.class).get())
                        .build());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("listCompletedPart: {}", completedParts.size());
        }
    }

    private ListPartsResponse listParts(S3 s3) {
        ListPartsRequest listRequest = ListPartsRequest.builder()
                    .bucket(s3.getBucket())
                    .key(s3.getKey())
                    .uploadId(s3.getUploadId())
                    .build();
        return s3Client.listParts(listRequest);
    }

    private void completePart(S3 s3, List<CompletedPart> completedParts) {
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
        if (log.isInfoEnabled()) {
            log.info("complete MultipartUpload");
        }
    }


    private void initClient(S3 s3) {
        if (s3Client != null && s3.isInit()) {
            return;
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
        if (StringUtils.isBlank(s3.getPlatform())) {
            s3.setPlatform(serviceconfig.getS3().getPlatform());
        }

        if (StringUtils.isBlank(s3.getRegion())) {
            s3.setRegion(serviceconfig.getS3().getRegion());
        }

        if (StringUtils.isBlank(s3.getEndpoint())) {
            s3.setEndpoint(serviceconfig.getS3().getEndpoint());
        }
//        SdkHttpClient.Builder builder = UrlConnectionHttpClient.builder()
//                .proxyConfiguration(ProxyConfiguration.builder()
//                        .useSystemPropertyValues(true)
//                        .build())
//                .connectionTimeout(Duration.ofSeconds(serviceconfig.getTimeout().getConnect()))
//                .socketTimeout(Duration.ofSeconds(serviceconfig.getTimeout().getSocket()));
        if(StringUtils.isNotBlank(s3.getIdToken()) && StringUtils.isBlank(s3.getAccessSecret())) {
            String[] jwtParts = s3.getIdToken().split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(jwtParts[1]));
            Map<String, Object> idToken = JsonUtils.toObject(payload, Map.class);
            if (idToken.get("sub").equals(s3.getBucket())) {
                getStsToken(s3, builder);
            }
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
