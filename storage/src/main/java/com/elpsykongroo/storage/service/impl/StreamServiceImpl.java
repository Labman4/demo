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
import com.elpsykongroo.base.domain.message.Send;
import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.base.service.KafkaService;
import com.elpsykongroo.base.utils.MessageDigestUtils;
import com.elpsykongroo.base.utils.NormalizedUtils;
import com.elpsykongroo.storage.service.S3Service;
import com.elpsykongroo.storage.service.StreamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class StreamServiceImpl implements StreamService {

    private final Map<String, List<String>> consumerMap = new ConcurrentHashMap<>();

    private final Map<String, String> uploadMap = new ConcurrentHashMap<>();

    @Autowired
    private KafkaService kafkaService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ServiceConfig serviceConfig;

    @Override
    public String checkSha256(S3 s3) {
        if (StringUtils.isNotBlank(s3.getSha256()) && StringUtils.isNotBlank(s3.getPartCount()) && StringUtils.isNotBlank(s3.getPartNum())) {
            String topic = s3.getPlatform() + "-" + s3.getRegion() + "-" + s3.getBucket() + "-" + NormalizedUtils.topicNormalize(s3.getKey()) +  "-bytes";
            String consumerGroupS3Key = s3.getBucket() + "-" + s3.getKey() + "-consumerId";
            String consumerGroupId = getConsumerGroupId(s3, topic, consumerGroupS3Key, false);
            if (log.isDebugEnabled()) {
                log.debug("checkSha256 consumerId:{}", consumerMap.get(topic + "-consumerId"));
            }
            if (StringUtils.isNotBlank(consumerGroupId)) {
                String shaKey = consumerGroupId + "*" + s3.getKey() + "*" + s3.getPartCount() + "*" + s3.getPartNum();
                String sha256 = s3Service.getObject(s3.getClientId(), s3.getBucket(), shaKey);
                if (sha256.toLowerCase(Locale.US).equals(s3.getSha256())) {
                    return "";
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("checkSha256 sha256:{} not match with s3:{}", s3.getSha256(), sha256.toLowerCase(Locale.US));
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void uploadStream(String clientId, S3 s3, Integer num, String uploadId) throws IOException {
        int start = 0;
        int partCount = num;
        if (StringUtils.isNotBlank(s3.getPartCount())) {
            partCount = Integer.parseInt(s3.getPartCount());
        }
        String topic = s3.getPlatform() + "-" + s3.getRegion() + "-" +  s3.getBucket() + "-" + NormalizedUtils.topicNormalize(s3.getKey()) + "-bytes";
        String consumerGroupKey = topic + "-consumerId";
        String consumerGroupS3Key = s3.getBucket() + "-" + s3.getKey() + "-consumerId";
        String consumerGroupId = getConsumerGroupId(s3, topic, consumerGroupS3Key, true);
        if (log.isDebugEnabled()) {
            log.debug("uploadStream consumerGroupId:{}", consumerMap.get(consumerGroupKey));
        }
        if (StringUtils.isNotBlank(consumerGroupId)) {
            rejoinListener(s3.getBucket(), topic, Instant.now().toEpochMilli(), consumerGroupId);
        }
        byte[][] output = new byte[num][];
        /**
         * for platform which not store uploadId to upload with exist stored message, skip uploadStream
         */
        if ("minio".equals(s3.getPlatform())) {
            if (log.isWarnEnabled()) {
                log.warn("uploadStream need obtain uploadId extra:{}", uploadMap);
            }
            if (StringUtils.isNotBlank(consumerGroupId)) {
                if (!uploadMap.containsKey(consumerGroupId + "-uploadId")) {
                    HeadObjectResponse uploadIdHead = s3Service.headObject(clientId, s3.getBucket(), consumerGroupId + "-uploadId");
                    if (uploadIdHead == null) {
                        String flag = uploadMap.putIfAbsent(consumerGroupId + "-uploadId", uploadId);
                        if (flag == null) {
                            s3Service.uploadObject(clientId, s3.getBucket(), consumerGroupId + "-uploadId",
                                    RequestBody.fromString(uploadId));
                        }
                    }
                }
            } else {
                return;
            }
        }
        uploadPartByStream(clientId, s3, num, uploadId, consumerMap.get(consumerGroupKey).get(0), start, partCount, topic, output);
    }

    private void uploadPartByStream(String clientId, S3 s3, Integer num, String uploadId, String consumerGroupId, int start, int partCount, String topic, byte[][] output) throws IOException {
        int partSize = Integer.parseInt(s3.getPartSize());
        for(int i = start; i < num; i++) {
            int percent = (int) Math.ceil((double) i+1 / num * 100);
            if (log.isInfoEnabled()) {
                log.info("uploadPartByStream complete:{} ", percent + "%");
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
                log.debug("uploadPartByStream consumerGroupId:{}, part {}-{}",
                        consumerGroupId, partCount, partNum);
            }
            String shaKey = consumerGroupId + "*" +s3.getKey() + "*" + partCount + "*" + partNum;
            String sha256 = MessageDigestUtils.sha256(output[i]);
            HeadObjectResponse headObjectResponse = s3Service.headObject(clientId, s3.getBucket(), shaKey);
            Send send = new Send();
            send.setTopic(topic);
            send.setKey(key);
            send.setData(output[i]);
            send.setSha256(sha256);
            if (headObjectResponse == null) {
                String sendResult = kafkaService.send(send);
                if ("1".equals(sendResult)) {
                    s3Service.uploadObject(clientId, s3.getBucket(), shaKey, RequestBody.fromString(sha256));
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("uploadPartByStream sha256 have changed between transform");
                    }
                }
            } else {
                String sha = s3Service.getObject(clientId, s3.getBucket(), shaKey);
                if (sha256.equals(sha)) {
                    if (log.isInfoEnabled()) {
                        log.info("uploadPartByStream part:{} is completed", partNum);
                    }
                } else {
                    if (log.isWarnEnabled()) {
                        log.warn("uploadPartByStream part:{} sha256:{} is not match:{}, re-upload:{}", s3.getPartNum(), sha256, sha, shaKey);
                        kafkaService.send(send);
                    }
                }
            }
        }
    }

    private void startListener(String topic, String id, String consumerGroupId) {
        if (StringUtils.isNotBlank(id) && StringUtils.isNotBlank(consumerGroupId)) {
            if (log.isDebugEnabled()) {
                log.debug("start listen id:{}, groupId:{}", id , consumerGroupId);
            }
            List<String> consumerIds = null;
            if (consumerMap.containsKey(topic + "-" + consumerGroupId)) {
                consumerIds = consumerMap.get(topic + "-" + consumerGroupId);
            } else {
                consumerIds = new ArrayList<>();
            }
            consumerIds.add(id);
            consumerMap.put(topic + "-" + consumerGroupId, consumerIds);
            Send send = new Send();
            send.setCallback(serviceConfig.getUrl().getStorageCallback());
            send.setTopic(topic);
            send.setId(id);
            send.setGroupId(consumerGroupId);
            send.setManualStop(true);
            kafkaService.callback(send);
        }
    }

    private void rejoinListener(String bucket, String topic, long timestamp, String consumerGroupId) {
        if (log.isDebugEnabled()) {
            log.debug("add listener");
        }
        if (StringUtils.isNotBlank(consumerGroupId)) {
            startListener(topic, bucket + "-" + timestamp + "-" + Thread.currentThread().getId(), consumerGroupId);
        }
    }

    @Async
    @EventListener
    public void autoComplete(S3 s3) {
        if (StringUtils.isNotBlank(s3.getPartCount())) {
            List<CompletedPart> completedParts = new ArrayList<CompletedPart>();
            s3Service.listCompletedPart(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
            if (completedParts.size() == Integer.parseInt(s3.getPartCount())) {
                if (log.isDebugEnabled()) {
                    log.debug("autoCompletePart");
                }
                s3Service.completePart(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
                if (StringUtils.isNotBlank(s3.getConsumerGroupId())) {
                    completeTopic(s3);
                    s3Service.deleteObjectByPrefix(s3.getClientId(), s3.getBucket(), s3.getConsumerGroupId());
                    s3Service.deleteObject(s3.getClientId(), s3.getBucket(), s3.getBucket() + "-" + s3.getKey() + "-consumerId");
                }
            }
        }
    }

    private void completeTopic(S3 s3) {
        String topic = s3.getPlatform() + "-" + s3.getRegion() + "-" + s3.getBucket() + "-" + NormalizedUtils.topicNormalize(s3.getKey()) + "-bytes" ;
        String consumerGroupKey = topic + "-consumerId";
        String consumerGroupS3Key = s3.getBucket() + "-" + s3.getKey() + "-consumerId";
        String consumerGroupId = getConsumerGroupId(s3, topic, consumerGroupS3Key, false);
        if (log.isDebugEnabled()) {
            log.debug("start complete topic, consumerGroupId before:{}, after:{}", consumerMap.get(consumerGroupKey), consumerGroupId);
        }
        String consumerKey = topic + "-" + consumerGroupId ;
        List<String> consumerIds = new ArrayList<>();
        if (consumerMap.containsKey(consumerKey)) {
            consumerIds = consumerMap.get(consumerKey);
        }
        consumerIds.add(s3.getConsumerGroupId());
        kafkaService.stop(consumerIds.toString());
        clearMap(s3.getPlatform(), consumerGroupKey, consumerGroupId, consumerKey);
        String listenerState = kafkaService.listenerState(consumerIds.toString());
        if (log.isDebugEnabled()) {
            log.debug("listener state:{}", listenerState);
        }
        if ("false".equals(listenerState)) {
            if (log.isDebugEnabled()) {
                log.debug("delete topic:{}", topic);
            }
            kafkaService.deleteTopic(topic);
        } else {
            while ("true".equals(listenerState)) {
                listenerState = kafkaService.listenerState(consumerIds.toString());
                if (log.isDebugEnabled()) {
                    log.debug("wait all listener stop state:{}", listenerState);
                }
                if ("false".equals(listenerState)) {
                    if (log.isDebugEnabled()) {
                        log.debug("delete topic:{}", topic);
                    }
                    kafkaService.deleteTopic(topic);
                }
            }
        }
    }

    private String getConsumerGroupId(S3 s3, String topic, String consumerGroupS3Key, boolean init) {
        String consumerGroupKey = topic + "-consumerId";
        if (!consumerMap.containsKey(consumerGroupKey)) {
            HeadObjectResponse response = s3Service.headObject(s3.getClientId(), s3.getBucket(), consumerGroupS3Key);
            if (response != null) {
                if (log.isDebugEnabled()) {
                    log.debug("uploadStream consumerGroupId already store in s3");
                }
                String consumerGroupId = s3Service.getObject(s3.getClientId(), s3.getBucket(), consumerGroupS3Key);
                if (StringUtils.isNotBlank(consumerGroupId)) {
                    List<String> consumerIds = new ArrayList<>();
                    consumerIds.add(consumerGroupId);
                    consumerMap.putIfAbsent(consumerGroupKey, consumerIds);
                } else {
                    while (StringUtils.isBlank(consumerGroupId)) {
                        if (log.isDebugEnabled()) {
                            log.debug("uploadStream try to fetch consumerGroupId");
                        }
                        String id = s3Service.getObject(s3.getClientId(), s3.getBucket(), consumerGroupS3Key);
                        if (StringUtils.isNotBlank(id)) {
                            List<String> consumerId = new ArrayList<>();
                            consumerId.add(id);
                            consumerMap.putIfAbsent(consumerGroupKey, consumerId);
                            startListener(topic, s3.getBucket() + "-" + Instant.now().toEpochMilli() + "-" + Thread.currentThread().getId(), consumerGroupId);
                            return id;
                        }
                    }

                }
            } else if (init) {
                String consumerGroupId = s3.getBucket() + "-" + Instant.now().toEpochMilli();
                List<String> consumerId = new ArrayList<>();
                consumerId.add(consumerGroupId);
                List<String> flag = consumerMap.putIfAbsent(consumerGroupKey, consumerId);
                if (flag == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("getConsumerId, upload consumerId to s3");
                    }
                    s3Service.uploadObject(s3.getClientId(), s3.getBucket(), consumerGroupS3Key,
                            RequestBody.fromString(consumerGroupId));
                    List<String> consumerIds = new ArrayList<>();
                    consumerIds.add(consumerGroupId);
                    consumerMap.putIfAbsent(topic + "-" + consumerGroupId, consumerIds);
                    startListener(topic, consumerGroupId, consumerGroupId);
                    return consumerGroupId;
                }
            }
        }
        if (StringUtils.isNotBlank(consumerMap.get(consumerGroupKey).get(0))) {
            return consumerMap.get(consumerGroupKey).get(0);
        } else {
            return s3Service.getObject(s3.getClientId(), s3.getBucket(), consumerGroupS3Key);
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
}
