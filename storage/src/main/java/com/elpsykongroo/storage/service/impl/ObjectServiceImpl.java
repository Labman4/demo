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
import com.elpsykongroo.base.utils.MessageDigestUtils;
import com.elpsykongroo.base.utils.NormalizedUtils;
import com.elpsykongroo.storage.listener.ObjectListener;
import com.elpsykongroo.storage.service.ObjectService;
import java.util.Base64;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
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
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ObjectServiceImpl implements ObjectService {

    private final Map<String, S3Client> clientMap = new ConcurrentHashMap<>();

    private final Map<String, List<String>> consumerMap = new ConcurrentHashMap<>();

    private final Map<String, String> uploadMap = new ConcurrentHashMap<>();

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

//    private static List<ConsumerGroupListing> listConsumerGroups(String groupId, AdminClient adminClient) throws InterruptedException, ExecutionException {
//        ListConsumerGroupsResult result = adminClient.listConsumerGroups();
//        List<ConsumerGroupListing> groups = new ArrayList<>();
////        int count = 0;
//        for (ConsumerGroupListing consumerGroup : result.all().get()){
//            if (groupId.equals(consumerGroup.groupId())) {
//                if (log.isDebugEnabled()) {
//                    log.debug("consumerGroup: {}", consumerGroup.toString());
//                }
//                groups.add(consumerGroup);
////                if (consumerGroup.state().get() == ConsumerGroupState.EMPTY) {
////                    log.debug("consumerGroup: {}", consumerGroup.toString());
////                    count ++;
////                }
//            }
//        }
////        if (count == groups.size()) {
////            return false;
////        }
//        return groups;
//    }

    @Override
    public void multipartUpload(S3 s3) throws IOException {
        initClient(s3, "");
        if (StringUtils.isBlank(s3.getKey())) {
            String key = NormalizedUtils.topicNormalize(s3.getData()[0].getOriginalFilename());
            s3.setKey(key);
        }
        if (StringUtils.isBlank(s3.getUploadId())) {
            if (StringUtils.isNotBlank(obtainUploadId(s3))) {
                s3.setUploadId(obtainUploadId(s3));
            } else {
                return;
            }
        }
        uploadPart(s3, s3.getPlatform() + s3.getRegion() + s3.getBucket());
    }

    @Override
    public void download(S3 s3, HttpServletResponse response) throws IOException {
        initClient(s3, "");
        GetObjectRequest objectRequest = GetObjectRequest
                .builder()
                .bucket(s3.getBucket())
                .key(s3.getKey())
                .build();
        if (StringUtils.isNotBlank(s3.getOffset())) {
            long startPoint = Long.parseLong(s3.getOffset()); // 断点续传的起始位置
            objectRequest.toBuilder().range("bytes=" + startPoint + "-");
        }
        ResponseInputStream<GetObjectResponse> in =
                clientMap.get(s3.getPlatform() + s3.getRegion() + s3.getBucket()).getObject(objectRequest);
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

    @Override
    public void delete(S3 s3) {
        initClient(s3, "");
        deleteObject(s3.getPlatform() + s3.getRegion() + s3.getBucket(), s3.getBucket(), s3.getKey());
    }

    @Override
    public List<ListObject> list(S3 s3) {
        initClient(s3, "");
        List<ListObject> objects = new ArrayList<>();
        ListObjectsV2Iterable listResp = null;
        try {
            listResp = listObject(s3.getPlatform() + s3.getRegion() + s3.getBucket(), s3.getBucket(), "");
        } catch (NoSuchBucketException e) {
            if (log.isWarnEnabled()) {
                log.warn("bucket not exist");
            }
            if(createBucket(s3.getPlatform() + s3.getRegion() + s3.getBucket(), s3.getBucket())) {
                return objects;
            }
        }
        listResp.contents().stream()
                .forEach(content -> objects.add(new ListObject(content.key(),
                        content.lastModified(),
                        content.size())));
        return objects;
    }

    @Override
    public String obtainUploadId(S3 s3) throws IOException {
        initClient(s3, "");
        HeadObjectResponse headObjectResponse = headObject(s3.getPlatform() + s3.getRegion() + s3.getBucket(), s3.getBucket(), s3.getKey());
        if (headObjectResponse != null) {
            return "";
        }
        if (!"minio".equals(s3.getPlatform())) {
            List<MultipartUpload> uploads = listMultipartUploads(s3.getPlatform() + s3.getRegion() + s3.getBucket(), s3.getBucket()).uploads();
            for (MultipartUpload upload : uploads) {
                if (s3.getKey().equals(upload.key())) {
                    return upload.uploadId();
                }
            }
        }
        return createMultiPart(s3.getPlatform() + s3.getRegion() + s3.getBucket(), s3.getBucket(), s3.getKey()).uploadId();
    }

    private void uploadPart(S3 s3, String clientId) throws IOException {
        partSize = Math.max(Long.parseLong(s3.getPartSize()), 5 * 1024 * 1024);
        RequestBody requestBody = null;
        long fileSize = 0;
        String sha256 = "";
        int num = 1;
        if (s3.getByteData() != null) {
            sha256 = MessageDigestUtils.sha256(s3.getByteData());
            requestBody = RequestBody.fromBytes(s3.getByteData());
            fileSize = s3.getByteData().length;
            num = (int) Math.ceil((double) fileSize / partSize);
        } else {
            sha256 = MessageDigestUtils.sha256(s3.getData()[0].getBytes());
            requestBody = RequestBody.fromBytes(s3.getData()[0].getBytes());
            fileSize = s3.getData()[0].getSize();
            num = (int) Math.ceil((double) fileSize / partSize);
            if ("stream".equals(s3.getMode()) && (fileSize >= partSize || StringUtils.isNotBlank(s3.getPartNum()))) {
                uploadStream(clientId, s3, num, s3.getUploadId());
            }
        }
        if (fileSize < partSize && StringUtils.isEmpty(s3.getPartNum())) {
            uploadObject(clientId, s3.getBucket(), s3.getKey(), requestBody);
            return;
        }
        if(!"stream".equals(s3.getMode())) {
            List<CompletedPart> completedParts = new ArrayList<CompletedPart>();
            int startPart = 0;
            if ("minio".equals(s3.getPlatform())) {
                String uploadId = getObject(clientId, s3.getBucket(), s3.getConsumerId() + "-uploadId");
                if (log.isInfoEnabled()) {
                    log.info("uploadPart consumerGroupId:{}, uploadId:{}", s3.getConsumerId(), uploadId);
                }
                if (StringUtils.isNotBlank(uploadId)) {
                    s3.setUploadId(uploadId);
                }
            }
            listCompletedPart(clientId, s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
            if (!completedParts.isEmpty() && completedParts.size() < num) {
                // only work when upload without chunk
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
                boolean flag = false;
                for (CompletedPart part : completedParts) {
                    if (part.partNumber() == partNum) {
                        flag = true;
                    }
                }
                if(!flag) {
                    String shaKey = s3.getConsumerId() + "*" + s3.getKey() + "*" + s3.getPartCount() + "*" + (partNum - 1);
                    String sha = getObject(clientId, s3.getBucket(), shaKey);
                    if (sha256.equals(sha)) {
                        UploadPartResponse uploadPartResponse = uploadPart(clientId, s3, requestBody, partNum, endOffset);
                        if (uploadPartResponse != null) {
                            completedParts.add(
                                    CompletedPart.builder()
                                            .partNumber(partNum)
                                            .eTag(uploadPartResponse.eTag())
                                            .build()
                            );
                        }
                    } else {
                        if (log.isInfoEnabled()) {
                            log.info("sha256:{} not match with: {}", sha256, shaKey);
                        }
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("part:{} is complete, skip", partNum);
                    }
                }
            }
            if (StringUtils.isNotBlank(s3.getPartCount())) {
                completedParts = new ArrayList<CompletedPart>();
                listCompletedPart(clientId, s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
                if (completedParts.size() == Integer.parseInt(s3.getPartCount())) {
                    completePart(clientId, s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
                    if (StringUtils.isNotBlank(s3.getConsumerId())) {
                        completeTopic(s3, clientId);
                        deleteObjectByPrefix(clientId, s3.getBucket(), s3.getConsumerId());
                        deleteObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId");
                    }
                }
            } else if (completedParts.size() == num) {
                completePart(clientId, s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
            }
        }
    }

    private void uploadStream(String clientId, S3 s3, Integer num, String uploadId) throws IOException {
        long timestamp = Instant.now().toEpochMilli();
        int start = 0;
        int partCount = num;
        if (StringUtils.isNotBlank(s3.getPartCount())) {
            partCount = Integer.parseInt(s3.getPartCount());
        }
        String topic = s3.getPlatform() + "-" + s3.getRegion() + "-" +  s3.getBucket() + "-" + s3.getKey();
        String consumerGroupKey = topic + "-consumerId";
        if (!consumerMap.containsKey(consumerGroupKey)) {
            HeadObjectResponse response = headObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId");
            if (response == null) {
                List<String> consumerGroupId = new ArrayList<>();
                consumerGroupId.add(s3.getBucket() + "-" + timestamp);
                List<String> flag = consumerMap.putIfAbsent(consumerGroupKey, consumerGroupId);
                if (flag == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("upload consumerId to s3");
                    }
                    uploadObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId",
                            RequestBody.fromString(s3.getBucket() + "-" + timestamp));
                    startListener(topic, s3.getBucket() + "-" + timestamp, s3.getBucket() + "-" + timestamp);
                    List<String> consumerIds = new ArrayList<>();
                    consumerIds.add(s3.getBucket() + "-" + timestamp);
                    consumerMap.putIfAbsent(topic + s3.getBucket() + "-" + timestamp, consumerIds);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("consumerGroupId already store in s3");
                }
                while (consumerMap.get(consumerGroupKey) == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("try to fetch consumerGroupId");
                    }
                    String consumerGroupId = getObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId");
                    if (StringUtils.isNotBlank(consumerGroupId)) {
                        List<String> consumerIds = new ArrayList<>();
                        consumerIds.add(consumerGroupId);
                        consumerMap.putIfAbsent(consumerGroupKey, consumerIds);
                        startListener(topic, s3.getBucket() + "-" + timestamp + "-" + Thread.currentThread().getId(), consumerGroupId);
                        resetOffset(consumerGroupId);
                    }
                }
            }
        }
        byte[][] output = new byte[num][];
        String obtainUploadId = obtainUploadId(s3);
        if (StringUtils.isBlank(obtainUploadId)) {
            return;
        }
        String consumerGroupId = consumerMap.get(consumerGroupKey).get(0);
        if (!uploadId.equals(obtainUploadId)) {
            if (log.isWarnEnabled()) {
                log.warn("uploadId not exist");
                if (!uploadMap.containsKey(consumerGroupId + "-uploadId")) {
                    HeadObjectResponse uploadIdHead = headObject(clientId, s3.getBucket(), consumerGroupId + "-uploadId");
                    if (uploadIdHead == null) {
                        String flag = uploadMap.putIfAbsent(consumerGroupId + "-uploadId", uploadId);
                        if(flag == null) {
                            uploadObject(clientId, s3.getBucket(), consumerGroupId + "-uploadId",
                                    RequestBody.fromString(uploadId));
                        }
                    }
                }
            }
        }
        uploadPartByStream(clientId, s3, num, uploadId, consumerGroupId, start, partCount, topic, output);
    }

    private void resetOffset(String consumerGroupId) {
        AdminClient adminClient =  AdminClient.create(kafkaAdmin.getConfigurationProperties());
        ListConsumerGroupOffsetsResult result = adminClient.listConsumerGroupOffsets(consumerGroupId);
        try {
            if (log.isDebugEnabled()) {
                log.debug("manual reset offset");
            }
            Map<TopicPartition, OffsetAndMetadata> offsets = result.partitionsToOffsetAndMetadata().get();
            for (TopicPartition partition: offsets.keySet()) {
                offsets.put(partition, new OffsetAndMetadata(0));
            }
            adminClient.alterConsumerGroupOffsets(consumerGroupId, offsets);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("reset offset error: {}", e.getMessage());
            }
        }  finally {
            adminClient.close();
        }
    }

    private void startListener(String topic, String id, String consumerGroupId) {
        try {
            if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(consumerGroupId)) {
                MessageListenerContainer container = endpointRegistry.getListenerContainer(id);
                if (container == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("start listen id:{}, groupId:{}", id , consumerGroupId);
                    }
                    List<String> consumerIds = null;
                    if (consumerMap.containsKey(topic + consumerGroupId)) {
                        consumerIds = consumerMap.get(topic + consumerGroupId);
                    } else {
                        consumerIds = new ArrayList<>();
                    }
                    consumerIds.add(id);
                    consumerMap.putIfAbsent(topic + consumerGroupId, consumerIds);
                    ac.getBean(ObjectListener.class, id, topic, consumerGroupId, this);
                }
            }
            }catch(BeansException e){
                if (log.isWarnEnabled()) {
                    log.warn("already on listen:{}", e.getMessage());
                }
            }
    }

    private void uploadPartByStream(String clientId, S3 s3, Integer num, String uploadId, String consumerGroupId, int start, int partCount, String topic, byte[][] output) throws IOException {
        for(int i = start; i < num; i++) {
            int percent = (int) Math.ceil((double) i / num * 100);
            if (log.isInfoEnabled()) {
                log.info("uploadStream complete:{} ", percent + "%");
            }
            int partNum = i;
            if (StringUtils.isNotBlank(s3.getPartNum())) {
                partNum = Integer.parseInt(s3.getPartNum());
            }
            String key = s3.getPlatform() + "*" + s3.getRegion() + "*" + consumerGroupId + "*" + s3.getKey() + "*" + partCount + "*" + partNum + "*" + uploadId;
            long startOffset = i * partSize;
            long endOffset = startOffset + Math.min(partSize, s3.getData()[0].getSize() - startOffset);
            output[i] = Arrays.copyOfRange(s3.getData()[0].getBytes(), (int) startOffset, (int) endOffset);
            if (log.isDebugEnabled()) {
                log.debug("uploadStream part {}-{} ", partCount, partNum);
            }
            String shaKey = consumerGroupId + "*" +s3.getKey() + "*" + partCount + "*" + partNum;
            String sha256 = MessageDigestUtils.sha256(output[i]);
            HeadObjectResponse headObjectResponse = headObject(clientId, s3.getBucket(), shaKey);
            if (headObjectResponse == null) {
                CompletableFuture<SendResult<String, Object>> result = kafkaTemplate.send(topic, key, output[i]);
                try {
                    String recordSha256 = MessageDigestUtils.sha256((byte[])result.get().getProducerRecord().value());
                    if (log.isDebugEnabled()) {
                        log.debug("record sha256: {}", recordSha256);
                    }
                    if (sha256.equals(recordSha256)) {
                        uploadObject(clientId, s3.getBucket(), shaKey, RequestBody.fromString(sha256));
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("get send result error:{}", e.getMessage());
                    }
                }
            } else {
                String sha = getObject(clientId, s3.getBucket(), shaKey);
                if (sha256.equals(sha)) {
                    if (log.isInfoEnabled()) {
                        log.info("part:{} is completed", s3.getPartNum());
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("part:{} sha256 is not match, re-upload ", s3.getPartNum());
                        kafkaTemplate.send(topic, key, output[i]);
                    }
                }
            }
        }
    }

    private void completeTopic(S3 s3, String clientId) {
        String topic = s3.getPlatform() + "-" + s3.getRegion() + "-" + s3.getBucket() + "-" + s3.getKey();
        String consumerGroupKey = topic + "-consumerId";
        String consumerGroupId = "";
        if (!consumerMap.containsKey(consumerGroupKey)) {
            consumerGroupId = getObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId");
        } else {
            consumerGroupId = consumerMap.get(consumerGroupKey).get(0);
        }
        String consumerKey = topic + consumerGroupId ;
        if (consumerMap.containsKey(consumerKey)) {
            List<String> consumerIds = consumerMap.get(consumerKey);
            if (log.isDebugEnabled()) {
                log.debug("consumerIds: {}", consumerIds.toString());
            }
            for (String consumer: consumerIds) {
                MessageListenerContainer container = endpointRegistry.getListenerContainer(consumer);
                if (container != null) {
                    if (container.isRunning()) {
                        container.stop();
                        clearMap(s3.getPlatform(), consumerGroupKey, consumerGroupId, consumerKey);
                    }
                }
            }
        } else {
            MessageListenerContainer container = endpointRegistry.getListenerContainer(s3.getConsumerId());
            if (container != null) {
                if (container.isRunning()) {
                    container.stop();
                    clearMap(s3.getPlatform(), consumerGroupKey, consumerGroupId, consumerKey);
                }
            }
        }
        AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
        try {
            if (log.isDebugEnabled()) {
                log.debug("delete topic: {}", topic);
            }
            adminClient.deleteTopics(Collections.singleton(topic));
        } finally {
            adminClient.close();
        }
    }

    private void clearMap(String platform, String consumerGroupKey, String consumerGroupId, String consumerKey) {
        consumerMap.remove(consumerGroupKey);
        consumerMap.remove(consumerKey);
        if ("minio".equals(platform) && consumerMap.containsKey(consumerGroupId + "-uploadId")) {
            consumerMap.remove(consumerGroupId + "-uploadId");
        }
    }

    private void uploadObject(String clientId, String bucket, String key, RequestBody requestBody) throws IOException {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        clientMap.get(clientId).putObject(objectRequest, requestBody).eTag();
    }

    private void deleteObject(String clientId, String bucket, String key) {
        if (log.isDebugEnabled()) {
            log.debug("deleteObject key:{}", key);
        }
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        clientMap.get(clientId).deleteObject(deleteObjectRequest);
    }

    private void deleteObjectByPrefix(String clientId, String bucket, String prefix) {
        List<ObjectIdentifier> toDelete = new ArrayList<>();
        listObject(clientId, bucket, prefix).contents().stream().forEach(obj -> toDelete.add(ObjectIdentifier.builder()
                .key(obj.key()).build()));
        DeleteObjectsRequest deleteObjectRequest = DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(toDelete).build())
                .build();
        clientMap.get(clientId).deleteObjects(deleteObjectRequest);
    }

    private String getObject(String clientId, String bucket, String key) {
        try {
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            ResponseBytes<GetObjectResponse> resp = clientMap.get(clientId).getObjectAsBytes(objectRequest);
            if (resp != null) {
                String str = new String(resp.asByteArray());
                if (log.isDebugEnabled()) {
                    log.debug("getObjectAsBytes value:{}",str);
                }
                return str;
            }
        } catch (NoSuchKeyException e) {
            if (log.isErrorEnabled()) {
                log.error("getObjectAsBytes error:{} key:{}", e.getMessage(), key);
            }
            return "";
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

    private ListObjectsV2Iterable listObject(String clientId, String bucket, String prefix) {
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();
        return clientMap.get(clientId).listObjectsV2Paginator(listReq);
    }

    private CreateMultipartUploadResponse createMultiPart(String clientId, String bucket, String key) {
        if (log.isTraceEnabled()) {
            log.trace("create multipartUpload");
        }
        CreateMultipartUploadRequest createMultipartUploadRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        return clientMap.get(clientId).createMultipartUpload(createMultipartUploadRequest);
    }

    public boolean createBucket(String clientId, String bucket) {
        try {
            S3Waiter s3Waiter = clientMap.get(clientId).waiter();
            CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                    .bucket(bucket)
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

    private HeadObjectResponse headObject(String clientId, String bucket, String key) {
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

    private UploadPartResponse uploadPart(String clientId, S3 s3, RequestBody requestBody, int partNum, long endOffset) throws IOException {
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
            String uploadId = obtainUploadId(s3);
            if (StringUtils.isNotBlank(uploadId)) {
                uploadRequest = UploadPartRequest.builder()
                        .bucket(s3.getBucket())
                        .key(s3.getKey())
                        .uploadId(uploadId)
                        .partNumber(partNum)
                        .contentLength(endOffset)
                        .build();
                return clientMap.get(clientId).uploadPart(uploadRequest, requestBody);
            } else {
                return null;
            }
        } catch (SdkClientException e) {
            return clientMap.get(clientId).uploadPart(uploadRequest, requestBody);
        }
    }

    private ListMultipartUploadsResponse listMultipartUploads(String clientId, String bucket) {
        ListMultipartUploadsRequest listMultipartUploadsRequest = ListMultipartUploadsRequest.builder()
                .bucket(bucket)
                .build();
        ListMultipartUploadsResponse resp = clientMap.get(clientId).listMultipartUploads(listMultipartUploadsRequest);
        if (log.isDebugEnabled()) {
            log.debug("listMultipartUploads: {}", resp.uploads().size());
        }
        return resp;
    }

//    private void abortMultipartUpload (String bucket, String key, String uploadId) {
//        AbortMultipartUploadRequest abortMultipartUploadRequest = AbortMultipartUploadRequest.builder()
//                        .bucket(bucket)
//                        .key(key)
//                        .uploadId(uploadId)
//                        .build();
//        s3Client.abortMultipartUpload(abortMultipartUploadRequest);
//    }

    private void listCompletedPart(String clientId, String bucket, String key, String uploadId, List<CompletedPart> completedParts) {
        ListPartsResponse listPartsResponse = listParts(clientId, bucket, key, uploadId);
        if (listPartsResponse != null && listPartsResponse.parts().size() > 0) {
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

    private ListPartsResponse listParts(String clientId, String bucket, String key, String uploadId) {
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
        } catch (AwsServiceException e) {
            if (log.isErrorEnabled()) {
                log.error("listPart awsService error: {}", e.awsErrorDetails());
            }
        } catch (SdkClientException e) {
            if (log.isErrorEnabled()) {
                log.error("listPart sdk client error: {}", e.getMessage());
            }
        }
        return null;
    }

    private void completePart(String clientId, String bucket, String key, String uploadId, List<CompletedPart> completedParts) {
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

    private void initClient(S3 s3, String clientId) {

        if (StringUtils.isBlank(s3.getPlatform())) {
            s3.setPlatform(serviceconfig.getS3().getPlatform());
        }

        if (StringUtils.isBlank(s3.getRegion())) {
            s3.setRegion(serviceconfig.getS3().getRegion());
        }

        if (StringUtils.isNotBlank(s3.getKey())) {
            s3.setKey(NormalizedUtils.topicNormalize(s3.getKey()));
        }

        if (StringUtils.isBlank(clientId)) {
            clientId = s3.getPlatform() + s3.getRegion() + s3.getBucket();
        }

        if (clientMap.containsKey(clientId)) {
            if (!uploadMap.containsKey(clientId + "-timestamp")) {
                if (log.isTraceEnabled()) {
                    log.trace("skip init");
                }
                return;
            } else {
                String timestamp = uploadMap.get(clientId + "-timestamp");
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
        uploadMap.put(clientId + "-timestamp", String.valueOf(exp));

//            StsAssumeRoleWithWebIdentityCredentialsProvider provider = StsAssumeRoleWithWebIdentityCredentialsProvider.builder()
//                    .refreshRequest(awRequest)
//                    .stsClient(stsClient)
//                    .build();
        }
}
