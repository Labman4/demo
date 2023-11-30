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
import com.elpsykongroo.base.domain.message.Message;
import com.elpsykongroo.base.domain.storage.object.CorsRule;
import com.elpsykongroo.base.domain.storage.object.ListObjectResult;
import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.base.service.MessageService;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.utils.BytesUtils;
import com.elpsykongroo.base.utils.EncryptUtils;
import com.elpsykongroo.base.utils.MessageDigestUtils;
import com.elpsykongroo.base.utils.NormalizedUtils;
import com.elpsykongroo.base.utils.PkceUtils;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;

import com.elpsykongroo.storage.service.ObjectService;
import com.elpsykongroo.storage.service.S3Service;
import com.elpsykongroo.storage.service.StreamService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.GetBucketCorsResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ObjectServiceImpl implements ObjectService {
    private final ThreadLocal<Integer> count = new ThreadLocal<>();
    @Autowired
    private Map<String, S3Client> clientMap;

    @Autowired
    private ServiceConfig serviceconfig;

    @Autowired
    private RedisService redisService;

    @Autowired
    private StreamService streamService;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ApplicationContext ac;

    @Override
    public String multipartUpload(S3 s3) throws IOException {
        s3Service.initClient(s3, "");
        if (StringUtils.isBlank(s3.getKey())) {
            s3.setKey(s3.getData()[0].getOriginalFilename());
        }
        if (StringUtils.isBlank(s3.getUploadId())) {
            if (StringUtils.isNotBlank(obtainUploadId(s3))) {
                s3.setUploadId(obtainUploadId(s3));
            } else {
                return "0";
            }
        }
        List<CompletedPart> completedParts = uploadPart(s3);
        ac.publishEvent(s3);
        return String.valueOf(completedParts.size());
    }

    @Override
    public void abortMultipartUpload(S3 s3) {
        s3Service.initClient(s3, "");
        s3Service.abortMultipartUpload(clientMap.get(s3.getClientId()), s3.getBucket(), s3.getKey(), s3.getUploadId());
    }
    @Override
    public void download(S3 s3, HttpServletRequest request, HttpServletResponse response) throws IOException {
        s3Service.initClient(s3, "");
        downloadStream(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getOffset(), s3.getSecret(), "", "", request, response);
    }

    @Override
    public void delete(S3 s3) {
        s3Service.initClient(s3, "");
        s3Service.deleteObjects(clientMap.get(s3.getClientId()), s3.getBucket(), s3.getKey());
    }

    @Override
    public List<ListObjectResult> list(S3 s3) {
        s3Service.initClient(s3, "");
        List<ListObjectResult> objects = new ArrayList<>();
        ListObjectsV2Iterable listResp = null;
        try {
            listResp = s3Service.listObject(clientMap.get(s3.getClientId()), s3.getBucket(), "");
            if (listResp != null) {
                listResp.contents().stream()
                        .forEach(content -> objects.add(new ListObjectResult(content.key(),
                                content.lastModified(),
                                content.size())));
            }
        } catch (NoSuchBucketException e) {
            if (log.isWarnEnabled()) {
                log.warn("bucket not exist");
            }
            if(s3Service.createBucket(clientMap.get(s3.getClientId()), s3.getPlatform(), s3.getBucket())) {
                return objects;
            }
        }
        return objects;
    }

    @Override
    public List<CorsRule> getCorsRule(S3 s3) {
        s3Service.initClient(s3, "");
        List<CorsRule> corsRules = new ArrayList<>();
        GetBucketCorsResponse response = s3Service.getCorsRule(clientMap.get(s3.getClientId()), s3.getBucket());
        if (response != null && response.hasCorsRules()) {
            response.corsRules().stream().forEach(cors -> corsRules.add(new CorsRule(
                    cors.allowedHeaders(),
                    cors.allowedMethods(),
                    cors.allowedOrigins(),
                    cors.exposeHeaders(),
                    cors.maxAgeSeconds(),
                    cors.id())));
        }
        return corsRules;
    }

    @Override
    public void putCorsRule(S3 s3) {
        s3Service.initClient(s3, "");
        List<CORSRule> corsRules = new ArrayList<>();
        s3.getCorsRules().stream().forEach(corsRule -> corsRules.add(
                CORSRule.builder().allowedHeaders(corsRule.getAllowedHeaders())
                        .allowedMethods(corsRule.getAllowedMethods())
                        .allowedOrigins(corsRule.getAllowedOrigins())
                        .exposeHeaders(corsRule.getExposeHeaders())
                        .maxAgeSeconds(corsRule.getMaxAgeSeconds()).build()));
        s3Service.putCorsRule(clientMap.get(s3.getClientId()), s3.getBucket(), corsRules);
    }

    @Override
    public String getObjectUrl(S3 s3) {
        s3Service.initClient(s3, "");
        String plainText = s3.getPlatform() + "*" + s3.getRegion() + "*" + s3.getBucket();
        byte[] key = BytesUtils.generateRandomByte(16);
        byte[] ciphertext = EncryptUtils.encryptString(plainText, key);
        String cipherBase64 = Base64.getUrlEncoder().encodeToString(ciphertext);
        String keyBase64 = Base64.getUrlEncoder().encodeToString(key);
        String codeVerifier;
        if(StringUtils.isBlank(s3.getSecret())) {
            codeVerifier = PkceUtils.generateVerifier();
            String codeChallenge = PkceUtils.generateChallenge(codeVerifier);
            redisService.set("PKCE-" + codeVerifier, codeChallenge, serviceconfig.getTimeout().getPublicKey());
        } else {
            codeVerifier = s3.getSecret();
        }
        redisService.set(s3.getKey() + "-secret", keyBase64, serviceconfig.getTimeout().getStorageUrl());
        return serviceconfig.getUrl().getObject() +"?key="+ s3.getKey() + "&code=" + cipherBase64 + "&state=" + codeVerifier;
    }

    @Override
    public void getObjectByCode(String code, String state, String key, String offset, String secret, String algorithm, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String codeChallenge = redisService.get("PKCE-" + state);
        if(StringUtils.isNotBlank(codeChallenge) && codeChallenge.equals(PkceUtils.verifyChallenge(state))) {
            String keyData = redisService.get(key + "-secret");
            byte[] ciphertext = Base64.getUrlDecoder().decode(code);
            byte[] keyBytes = Base64.getUrlDecoder().decode(keyData);
            String plainText = EncryptUtils.decrypt(ciphertext, keyBytes);
            String[] keys = plainText.split("\\*");
            downloadStream(plainText, keys[2], key, offset, secret, algorithm, state, request, response);
        }
    }

    @Override
    public String receiveMessage(Message message) throws IOException {
        if (software.amazon.awssdk.utils.StringUtils.isNotBlank(message.getKey())) {
            if (log.isDebugEnabled()) {
                log.debug("start receive message, key:{}, data:{}", message.getKey(), message.getData().length);
            }
            String[] keys = message.getKey().split("\\*");
            S3 s3 = new S3();
            s3.setByteData(message.getData());
            s3.setPlatform(keys[0]);
            s3.setRegion(keys[1]);
            s3.setBucket(keys[2].split("-")[0]);
            s3.setConsumerGroupId(keys[2]);
            s3.setKey(keys[3]);
            s3.setPartCount(keys[4]);
            s3.setPartNum(keys[5]);
            s3.setUploadId(keys[6]);
            try {
                if (count.get() == null) {
                    count.set(0);
                }
                if (count.get() <= 3) {
                    return multipartUpload(s3);
                }
            } catch (Exception e) {
                count.set(count.get() + 1);
                multipartUpload(s3);
            }
        }
        return "0";
    }

    @Override
    public String obtainUploadId(S3 s3) {
        s3Service.initClient(s3, "");
        String match = streamService.checkSha256(s3);
        if (log.isDebugEnabled()) {
            log.debug("sha256 match result:{}", match);
        }
        if (match != null) {
            return match;
        }
        if (!"minio".equals(s3.getPlatform())) {
            List<MultipartUpload> uploads = s3Service.listMultipartUploads(clientMap.get(s3.getClientId()), s3.getPlatform(), s3.getBucket()).uploads();
            for (MultipartUpload upload : uploads) {
                if (s3.getKey().equals(upload.key())) {
                    return upload.uploadId();
                }
            }
        }
        return s3Service.createMultiPart(clientMap.get(s3.getClientId()), s3.getBucket(), s3.getKey()).uploadId();
    }

    private List<CompletedPart> uploadPart(S3 s3) throws IOException {
        List<CompletedPart> completedParts = new ArrayList<CompletedPart>();
        long partSize = Long.parseLong(s3.getPartSize());
        if ("minio".equals(s3.getPlatform())) {
            partSize = Math.max(partSize, 5 * 1024 & 1024);
        }
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
                streamService.uploadStream(s3.getClientId(), s3, num, s3.getUploadId());
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("fileSize:{}, pageSize:{}", fileSize, partSize);
        }
        if (fileSize < partSize && StringUtils.isEmpty(s3.getPartNum())) {
            String eTag = s3Service.uploadObject(clientMap.get(s3.getClientId()), s3.getBucket(), s3.getKey(), requestBody);
            completedParts.add(
                    CompletedPart.builder()
                            .partNumber(1)
                            .eTag(eTag)
                            .build()
            );
            return completedParts;
        }
        if(!"stream".equals(s3.getMode())) {
            int startPart = 0;
            if ("minio".equals(s3.getPlatform())) {
                String uploadId = s3Service.getObjectString(clientMap.get(s3.getClientId()), s3.getBucket(), s3.getConsumerGroupId() + "-uploadId");
                if (log.isInfoEnabled()) {
                    log.info("uploadPart consumerGroupId:{}, uploadId:{}", s3.getConsumerGroupId(), uploadId);
                }
                if (StringUtils.isNotBlank(uploadId)) {
                    s3.setUploadId(uploadId);
                }
            }
            s3Service.listCompletedPart(clientMap.get(s3.getClientId()), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
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
                    if (StringUtils.isNotBlank(s3.getConsumerGroupId())) {
                        String shaKey = s3.getConsumerGroupId() + "*" + s3.getKey() + "*" + s3.getPartCount() + "*" + (partNum - 1);
                        String sha = s3Service.getObjectString(clientMap.get(s3.getClientId()), s3.getBucket(), shaKey);
                        if (StringUtils.isNotBlank(sha) && !sha256.equals(sha)) {
                            if (log.isInfoEnabled()) {
                                log.info("uploadPart sha256:{} not match with s3:{}, key:{}", sha256, sha, shaKey);
                                continue;
                            }
                        }
                    }
                    UploadPartResponse uploadPartResponse = s3Service.uploadPart(clientMap.get(s3.getClientId()), s3, requestBody, partNum, endOffset);
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
                        log.info("uploadPart, part:{} is complete, skip", partNum);
                    }
                }
            }
            if (StringUtils.isBlank(s3.getPartCount()) && completedParts.size() == num) {
                s3Service.completePart(clientMap.get(s3.getClientId()), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
            }
        }
        return completedParts;
    }

    private void downloadStream(String clientId, String bucket, String key, String offset, String secret, String algorithm, String state, HttpServletRequest request, HttpServletResponse response) throws IOException {
        int chunkOffset = 1024 * 1024 * 5;
        int start = 0;
        int end = 0;
        String range = request.getHeader("Range");
        if (StringUtils.isNotBlank(range)) {
            String[] ranges = range.replace("bytes=", "").split("-");
            start = Integer.parseInt(ranges[0]);
            if (ranges.length > 1) {
                end = Integer.parseInt(ranges[1]);
            }
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }
        int startOffset = start / chunkOffset;
        if (start != startOffset * chunkOffset) {
            start = startOffset * chunkOffset;
        }
        ResponseInputStream<GetObjectResponse> in = null;
        if (StringUtils.isNotBlank(offset)) {
            chunkOffset = Integer.parseInt(offset);
            HeadObjectResponse headObjectResponse = s3Service.headObject(clientMap.get(clientId), bucket, key);
            int size = headObjectResponse.contentLength().intValue() / chunkOffset + 1;
            if ("AES-GCM".equals(algorithm)) {
                in = s3Service.getObjectStream(clientMap.get(clientId), bucket, key, start + ((12 + 16) * startOffset), end);
                response.setContentLengthLong(in.response().contentLength() - ((12 + 16) * size));
            } else {
                in = s3Service.getObjectStream(clientMap.get(clientId), bucket, key, start + 16 * startOffset, end);
                response.setContentLengthLong(in.response().contentLength() - 16 * size);
            }
            String contentRange = "";
            if (startOffset != size) {
                contentRange = "bytes " + (startOffset * chunkOffset) + "-" + ((startOffset + 1) * chunkOffset - 1) + "/" + headObjectResponse.contentLength();
            } else {
                contentRange = "bytes " + (startOffset * chunkOffset) + "-" + (headObjectResponse.contentLength() - 1) + "/" + headObjectResponse.contentLength();
            }
            response.setHeader("Content-Range", contentRange);
        } else {
            in = s3Service.getObjectStream(clientMap.get(clientId), bucket, key, start, end);
            response.setHeader("Content-Range", in.response().contentRange());
        }
        OutputStream out = response.getOutputStream();
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Disposition", "attachment; filename=" + NormalizedUtils.topicNormalize(key));
        response.setHeader("Content-Type", in.response().contentType());
        /**
         effect: content-length will be unknown
         */
//        response.setHeader("Content-Encoding", "gzip");
        /**
         * effect: need send extra byte change, chunkSize will change dynamic by byte[] length
         */
//        response.setHeader("Transfer-Encoding", "chunked");

        try {
            handleIn(in, out, response, secret, algorithm, state, chunkOffset);
            out.flush();
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    private void handleIn(ResponseInputStream<GetObjectResponse> in, OutputStream out, HttpServletResponse response, String secret, String algorithm, String state, int chunkOffset) throws IOException {
        if (in != null) {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(out);
            try {
                if (StringUtils.isNotBlank(secret)) {
//                    int startOffset = 0;
//                    int endOffset = 0;
//                    String[] respRanges = null;
//                    if (StringUtils.isNotBlank(in.response().contentRange())) {
//                        String respRange = "";
//                        respRanges = in.response().contentRange().replace("bytes ", "").split("/");
//                        String[] contentRange = respRanges[0].split("-");
//                        startOffset = Integer.parseInt(contentRange[0]);
//                        endOffset = Integer.parseInt(contentRange[1]);
//                        if ("AES-GCM".equals(algorithm)) {
//                            endOffset = endOffset - 12 - 16;
//                        } else {
//                            endOffset = endOffset - 16;
//                        }
//                        respRange = "bytes= " + startOffset + "-" + endOffset + "/" + respRanges[1];
////                        response.setHeader("Content-Range", respRange);
//                    }
                    String secretBase64 = messageService.getMessage(state);
                    if (StringUtils.isNotBlank(secretBase64)) {
                        if (log.isDebugEnabled()) {
                            log.debug("start decrypt");
                        }
                        int chunkSize = 0;
                        byte[] cipherResult = Base64.getDecoder().decode(secret);
                        byte[] secretData = Base64.getDecoder().decode(secretBase64);
                        byte[] secretOrigin = EncryptUtils.decryptAsByte(cipherResult, secretData);
                        if ("AES-GCM".equals(algorithm)) {
                            chunkSize = chunkOffset + 12 + 16;
                        } else {
                            chunkSize = chunkOffset + 16;
                        }
                        int totalBytesRead = 0;
                        int bytesRead;
                        int len = 0;
                        byte[] b = new byte[chunkSize];
                        while((bytesRead = in.read(b, len, chunkSize - len))!= -1) {
                            if (bytesRead + len >= chunkSize) {
                                byte[] decryptData = EncryptUtils.decryptAsByte(b, MessageDigestUtils.sha256ByteArray(secretOrigin));
                                bufferedOutputStream.write(decryptData, 0, decryptData.length);
                                totalBytesRead = 0;
                                len = 0;
                            }  else {
                                len += bytesRead;
                                totalBytesRead += bytesRead;
                            }
                        }
                        if (bytesRead == -1) {
                            byte[] truncatedArray = Arrays.copyOfRange(b, 0, totalBytesRead);
                            byte[] decryptData = EncryptUtils.decryptAsByte(truncatedArray, MessageDigestUtils.sha256ByteArray(secretOrigin));
                            bufferedOutputStream.write(decryptData, 0, decryptData.length);
                        }
                    }
                } else {
                    response.setHeader("ETag", in.response().eTag());
                    response.setContentLengthLong(in.response().contentLength());
                    byte[] bufferDirect = new byte[1024];
                    int len;
                    while ((len = bufferedInputStream.read(bufferDirect)) != -1) {
                        bufferedOutputStream.write(bufferDirect, 0, len);
                    }
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug("output error:{}", e);
                }
            } finally {
                if (bufferedOutputStream != null) {
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                }
                if (bufferedInputStream != null) {
                    bufferedInputStream.close();
                }
            }
        }
    }
}
