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
import com.elpsykongroo.base.domain.storage.object.ListObjectResult;
import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.utils.BytesUtils;
import com.elpsykongroo.base.utils.EncryptUtils;
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.base.utils.MessageDigestUtils;
import com.elpsykongroo.base.utils.NormalizedUtils;
import com.elpsykongroo.base.utils.PkceUtils;
import com.elpsykongroo.storage.listener.ObjectListener;
import com.elpsykongroo.storage.service.ObjectService;
import java.util.Base64;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Locale;
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

    @Autowired
    private RedisService redisService;

    @Override
    public void multipartUpload(S3 s3) throws IOException {
        initClient(s3, "");
        if (StringUtils.isBlank(s3.getKey())) {
            s3.setKey(s3.getData()[0].getOriginalFilename());
        }
        if (StringUtils.isBlank(s3.getUploadId())) {
            if (StringUtils.isNotBlank(obtainUploadId(s3))) {
                s3.setUploadId(obtainUploadId(s3));
            } else {
                return;
            }
        }
        uploadPart(s3);
    }

    @Override
    public void download(S3 s3, HttpServletRequest request, HttpServletResponse response) throws IOException {
        initClient(s3, "");
        downloadStream(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getOffset(), request, response);
    }

    private void downloadStream(String clientId, String bucket, String key, String offset, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String range = request.getHeader("Range");
        if (StringUtils.isNotBlank(range)) {
            String[] ranges = range.replace("bytes=", "").split("-");
            offset = ranges[0];
        }
        ResponseInputStream<GetObjectResponse> in =
                getObjectStream(clientId, bucket, key, offset);
        if (in != null) {
            String filename = NormalizedUtils.topicNormalize(key);
            response.setHeader("Accept-Ranges", in.response().acceptRanges());
            response.setContentLengthLong(in.response().contentLength());
            response.setContentType(in.response().contentType());
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            response.setHeader("ETag", in.response().eTag());
            if (StringUtils.isNotBlank(offset) || StringUtils.isNotBlank(range)) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", in.response().contentRange());
            }
            BufferedInputStream inputStream = new BufferedInputStream(in);
            BufferedOutputStream out = null;
            try {
                out = new BufferedOutputStream(response.getOutputStream());
                byte[] b = new byte[1024];
                int len;
                while ((len = inputStream.read(b)) != -1) {
                    out.write(b, 0, len);
                }
            } finally {
                if (out != null) {
                    out.flush();
                    out.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
                if (in != null) {
                    in.close();
                }
            }
        }
    }

    @Override
    public void delete(S3 s3) {
        initClient(s3, "");
        deleteObject(s3.getClientId(), s3.getBucket(), s3.getKey());
    }

    @Override
    public List<ListObjectResult> list(S3 s3) {
        initClient(s3, "");
        List<ListObjectResult> objects = new ArrayList<>();
        ListObjectsV2Iterable listResp = null;
        try {
            listResp = listObject(s3.getClientId(), s3.getBucket(), "");
        } catch (NoSuchBucketException e) {
            if (log.isWarnEnabled()) {
                log.warn("bucket not exist");
            }
            if(createBucket(s3.getClientId(), s3.getBucket())) {
                return objects;
            }
        }
        listResp.contents().stream()
                .forEach(content -> objects.add(new ListObjectResult(content.key(),
                        content.lastModified(),
                        content.size())));
        return objects;
    }

    @Override
    public String getObjectUrl(S3 s3) throws IOException {
        initClient(s3, "");
        String plainText = s3.getPlatform() + "*" + s3.getRegion() + "*" + s3.getBucket();
        byte[] iv = BytesUtils.generateRandomByte(16);
        byte[] ciphertext = EncryptUtils.encrypt(plainText, iv);
        String cipherBase64 = Base64.getUrlEncoder().encodeToString(ciphertext);
        String ivBase64 = Base64.getUrlEncoder().encodeToString(iv);
        String codeVerifier = PkceUtils.generateVerifier();
        String codeChallenge = PkceUtils.generateChallenge(codeVerifier);
        redisService.set(s3.getKey() + "-challenge", codeChallenge, serviceconfig.getTimeout().getStorage());
        redisService.set(s3.getKey() + "-secret", ivBase64, serviceconfig.getTimeout().getStorage());
        return serviceconfig.getUrl().getObject() +"?key="+ s3.getKey() + "&code=" + cipherBase64 + "&state=" + codeVerifier;
    }

    @Override
    public void getObjectByCode(String code, String state, String key, String offset, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String codeChallenge = redisService.get(key + "-challenge");
        if(StringUtils.isNotBlank(codeChallenge) && codeChallenge.equals(PkceUtils.verifyChallenge(state))) {
            String ivBase64 = redisService.get(key + "-secret");
            byte[] ciphertext = Base64.getUrlDecoder().decode(code);
            byte[] iv = Base64.getUrlDecoder().decode(ivBase64);
            String plainText = EncryptUtils.decrypt(ciphertext, iv);
            String[] keys = plainText.split("\\*");
            downloadStream(plainText, keys[2], key, offset, request, response);
        }
    }

    @Override
    public String obtainUploadId(S3 s3) throws IOException {
        initClient(s3, "");
        String match = checkSha256(s3);
        if (log.isDebugEnabled()) {
            log.debug("sha256 match result:{}", match);
        }
        if (match != null) {
            return match;
        }
        if (!"minio".equals(s3.getPlatform())) {
            List<MultipartUpload> uploads = listMultipartUploads(s3.getClientId(), s3.getBucket()).uploads();
            for (MultipartUpload upload : uploads) {
                if (s3.getKey().equals(upload.key())) {
                    return upload.uploadId();
                }
            }
        }
        return createMultiPart(s3.getClientId(), s3.getBucket(), s3.getKey()).uploadId();
    }

    private String checkSha256(S3 s3) {
        if (StringUtils.isNotBlank(s3.getSha256()) && StringUtils.isNotBlank(s3.getPartCount()) && StringUtils.isNotBlank(s3.getPartNum())) {
            String consumerGroupKey = s3.getPlatform() + "-" + s3.getRegion() + "-" + s3.getBucket() + "-" + s3.getKey() + "-consumerId";
            String consumerId = "";
            if (!consumerMap.containsKey(consumerGroupKey)) {
                String consumerGroupId = getObject(s3.getClientId(), s3.getBucket(), s3.getKey() + "-consumerId");
                if (StringUtils.isNotBlank(consumerGroupId)) {
                    List<String> consumerIds = new ArrayList<>();
                    consumerIds.add(consumerGroupId);
                    consumerMap.putIfAbsent(consumerGroupKey, consumerIds);
                }
            } else {
                consumerId = consumerMap.get(consumerGroupKey).get(0);
            }
            if (log.isDebugEnabled()) {
                log.debug("checkSha256 consumerId:{}", consumerMap.get(consumerGroupKey));
            }
            if (StringUtils.isNotBlank(consumerId)) {
                String shaKey = consumerId + "*" + s3.getKey() + "*" + s3.getPartCount() + "*" + s3.getPartNum();
                String sha256 = getObject(s3.getClientId(), s3.getBucket(), shaKey);
                if (sha256.toLowerCase(Locale.US).equals(s3.getSha256())) {
                    return "";
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("sha256:{} not match with s3:{}", s3.getSha256(), sha256.toLowerCase(Locale.US));
                    }
                }
            }
        }
        return null;
    }

    private void uploadPart(S3 s3) throws IOException {
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
                uploadStream(s3.getClientId(), s3, num, s3.getUploadId());
            }
        }
        if (fileSize < partSize && StringUtils.isEmpty(s3.getPartNum())) {
            uploadObject(s3.getClientId(), s3.getBucket(), s3.getKey(), requestBody);
            return;
        }
        if(!"stream".equals(s3.getMode())) {
            List<CompletedPart> completedParts = new ArrayList<CompletedPart>();
            int startPart = 0;
            if ("minio".equals(s3.getPlatform())) {
                String uploadId = getObject(s3.getClientId(), s3.getBucket(), s3.getConsumerGroupId() + "-uploadId");
                if (log.isInfoEnabled()) {
                    log.info("uploadPart consumerGroupId:{}, uploadId:{}", s3.getConsumerGroupId(), uploadId);
                }
                if (StringUtils.isNotBlank(uploadId)) {
                    s3.setUploadId(uploadId);
                }
            }
            listCompletedPart(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
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
                    String shaKey = s3.getConsumerGroupId() + "*" + s3.getKey() + "*" + s3.getPartCount() + "*" + (partNum - 1);
                    String sha = getObject(s3.getClientId(), s3.getBucket(), shaKey);
                    if (sha256.equals(sha)) {
                        UploadPartResponse uploadPartResponse = uploadPart(s3.getClientId(), s3, requestBody, partNum, endOffset);
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
                            log.info("sha256:{} not match with s3:{}, key:{}", sha256, sha, shaKey);
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
                listCompletedPart(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
                if (completedParts.size() == Integer.parseInt(s3.getPartCount())) {
                    completePart(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
                    if (StringUtils.isNotBlank(s3.getConsumerGroupId())) {
                        completeTopic(s3.getClientId(), s3);
                        deleteObjectByPrefix(s3.getClientId(), s3.getBucket(), s3.getConsumerGroupId());
                        deleteObject(s3.getClientId(), s3.getBucket(), s3.getKey() + "-consumerId");
                    }
                }
            } else if (completedParts.size() == num) {
                completePart(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
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
        String topic = s3.getPlatform() + "-" + s3.getRegion() + "-" +  s3.getBucket() + "-" + NormalizedUtils.topicNormalize(s3.getKey());
        String consumerGroupKey = topic + "-consumerId";
        String consumerGroupId = "";
        if (!consumerMap.containsKey(consumerGroupKey)) {
            HeadObjectResponse response = headObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId");
            if (response == null) {
                List<String> consumerId = new ArrayList<>();
                consumerId.add(s3.getBucket() + "-" + timestamp);
                List<String> flag = consumerMap.putIfAbsent(consumerGroupKey, consumerId);
                if (flag == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("upload consumerId to s3");
                    }
                    uploadObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId",
                            RequestBody.fromString(s3.getBucket() + "-" + timestamp));
                    List<String> consumerIds = new ArrayList<>();
                    consumerIds.add(s3.getBucket() + "-" + timestamp);
                    consumerMap.putIfAbsent(topic + "-" + s3.getBucket() + "-" + timestamp, consumerIds);
                    startListener(topic, s3.getBucket() + "-" + timestamp, s3.getBucket() + "-" + timestamp);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("consumerGroupId already store in s3");
                }
                while (StringUtils.isBlank(consumerGroupId)) {
                    if (log.isDebugEnabled()) {
                        log.debug("try to fetch consumerGroupId");
                    }
                    String id = getObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId");
                    if (StringUtils.isNotBlank(id)) {
                        List<String> consumerId = new ArrayList<>();
                        consumerId.add(id);
                        consumerMap.putIfAbsent(topic + "-consumerId", consumerId);
                        consumerGroupId = id;
                        startListener(topic, s3.getBucket() + "-" + timestamp + "-" + Thread.currentThread().getId(), consumerGroupId);
                        resetOffset(consumerGroupId, 0);
                    }
                }
            }
        } else {
            if (StringUtils.isNotBlank(consumerMap.get(consumerGroupKey).get(0))) {
                consumerGroupId = consumerMap.get(consumerGroupKey).get(0);
            } else {
                consumerGroupId= getObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId");
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("uploadStream consumerGroupId:{}", consumerMap.get(consumerGroupKey));
        }
        rejoinListener(s3.getBucket(), topic, timestamp, consumerGroupId);
        byte[][] output = new byte[num][];
        String obtainUploadId = obtainUploadId(s3);
        if (StringUtils.isBlank(obtainUploadId)) {
            return;
        }
        if (!uploadId.equals(obtainUploadId)) {
            if (log.isWarnEnabled()) {
                log.warn("uploadId not exist:{}", uploadMap);
            }
            if (StringUtils.isNotBlank(consumerGroupId)) {
                if (!uploadMap.containsKey(consumerGroupId + "-uploadId")) {
                    HeadObjectResponse uploadIdHead = headObject(clientId, s3.getBucket(), consumerGroupId + "-uploadId");
                    if (uploadIdHead == null) {
                        String flag = uploadMap.putIfAbsent(consumerGroupId + "-uploadId", uploadId);
                        if (flag == null) {
                            uploadObject(clientId, s3.getBucket(), consumerGroupId + "-uploadId",
                                    RequestBody.fromString(uploadId));
                        }
                    }
                }
            } else {
                return;
            }
        }
        uploadPartByStream(clientId, s3, num, uploadId, consumerGroupId, start, partCount, topic, output);
    }

    private void rejoinListener(String bucket, String topic, long timestamp, String consumerGroupId) {
        if (log.isDebugEnabled()) {
            log.debug("add listener");
        }
        if (StringUtils.isNotBlank(consumerGroupId)) {
            startListener(topic, bucket + "-" + timestamp + "-" + Thread.currentThread().getId(), consumerGroupId);
            resetOffset(consumerGroupId, 0);
        }
    }

    private void resetOffset(String consumerGroupId, long offset) {
        AdminClient adminClient =  AdminClient.create(kafkaAdmin.getConfigurationProperties());
        try {
            if (log.isDebugEnabled()) {
                log.debug("manual reset offset");
            }
            ListConsumerGroupOffsetsResult result = adminClient.listConsumerGroupOffsets(consumerGroupId);
            Map<TopicPartition, OffsetAndMetadata> offsets = result.partitionsToOffsetAndMetadata().get();
            for (TopicPartition partition: offsets.keySet()) {
                if (log.isDebugEnabled()) {
                    log.debug("partition: {}, topic:{}, leaderEpoch: {}, offset before:{}",
                            partition.partition(),
                            partition.topic(),
                            offsets.get(partition).leaderEpoch().get(),
                            offsets.get(partition).offset());
                }
                if (offset > 0 ) {
                    offsets.put(partition, new OffsetAndMetadata(offset));
                } else {
                    offsets.put(partition, new OffsetAndMetadata(offsets.get(partition).offset() - 1));
                }
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
                    consumerMap.putIfAbsent(topic + "-" + consumerGroupId, consumerIds);
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
                        log.warn("part:{} sha256:{} is not match:{}, re-upload:{}", s3.getPartNum(), sha256, sha, shaKey);
                        kafkaTemplate.send(topic, key, output[i]);
                    }
                }
            }
        }
    }

    private void completeTopic(String clientId, S3 s3) {
        String topic = s3.getPlatform() + "-" + s3.getRegion() + "-" + s3.getBucket() + "-" + NormalizedUtils.topicNormalize(s3.getKey());
        String consumerGroupKey = topic + "-consumerId";
        String consumerGroupId = "";
        if (!consumerMap.containsKey(consumerGroupKey)) {
            consumerGroupId = getObject(clientId, s3.getBucket(), s3.getKey() + "-consumerId");
        } else {
            consumerGroupId = consumerMap.get(consumerGroupKey).get(0);
        }
        if (log.isDebugEnabled()) {
            log.debug("start complete topic, consumerGroupId before:{}, after:{}", consumerMap.get(consumerGroupKey), consumerGroupId);
        }
        String consumerKey = topic + "-" + consumerGroupId ;
        List<String> consumerIds = new ArrayList<>();
        consumerIds.add(s3.getConsumerGroupId());
        if (consumerMap.containsKey(consumerKey)) {
            consumerIds = consumerMap.get(consumerKey);
            if (log.isDebugEnabled()) {
                log.debug("consumerIds: {}", consumerIds.toString());
            }
        }
        stopListener(consumerIds);
        clearMap(s3.getPlatform(), consumerGroupKey, consumerGroupId, consumerKey);
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

    private void stopListener(List<String> consumerIds) {
        for (String consumer: consumerIds) {
            MessageListenerContainer container = endpointRegistry.getListenerContainer(consumer);
            if (container != null) {
                if (log.isDebugEnabled()) {
                    log.debug("consumerIds: {}", container.getGroupId());
                }
                if (container.isRunning()) {
                    container.stop();
                }
            }
        }
    }

    private void clearMap(String platform, String consumerGroupKey, String consumerGroupId, String consumerKey) {
        if (log.isDebugEnabled()) {
            log.debug("clear consumerMap before platform:{}, consumerGroupKey:{}, consumerGroupId:{}, consumerKey:{}",
                    platform,
                    consumerGroupKey,
                    consumerGroupId,
                    consumerKey);
            log.debug("clear consumerMap before:{}", consumerMap);
        }
        consumerMap.remove(consumerGroupKey);
        consumerMap.remove(consumerKey);
        if ("minio".equals(platform) && consumerMap.containsKey(consumerGroupId + "-uploadId")) {
            consumerMap.remove(consumerGroupId + "-uploadId");
        }
        if (log.isDebugEnabled()) {
            log.debug("clear consumerMap after:{}", consumerMap);
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
            ResponseBytes<GetObjectResponse> bytesResp = clientMap.get(clientId).getObjectAsBytes(objectRequest);
            if (bytesResp != null) {
                String str = new String(bytesResp.asByteArray());
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

    private ResponseInputStream<GetObjectResponse> getObjectStream(String clientId, String bucket, String key, String offset) {
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

        if (StringUtils.isBlank(clientId)) {
            clientId = s3.getPlatform() + "*" + s3.getRegion() + "*" + s3.getBucket();
            s3.setClientId(clientId);
        }
        if (log.isDebugEnabled()) {
            log.debug("clientMap before:{}", clientMap.keySet());
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
