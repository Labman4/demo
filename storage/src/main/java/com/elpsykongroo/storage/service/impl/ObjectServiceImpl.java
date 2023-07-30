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
import com.elpsykongroo.base.domain.storage.object.ListObjectResult;
import com.elpsykongroo.base.domain.storage.object.S3;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.utils.BytesUtils;
import com.elpsykongroo.base.utils.EncryptUtils;
import com.elpsykongroo.base.utils.MessageDigestUtils;
import com.elpsykongroo.base.utils.NormalizedUtils;
import com.elpsykongroo.base.utils.PkceUtils;
import com.elpsykongroo.storage.service.ObjectService;
import java.util.Base64;
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
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.MultipartUpload;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ObjectServiceImpl implements ObjectService {
    private final ThreadLocal<Integer> count = new ThreadLocal<>();

    @Autowired
    private ServiceConfig serviceconfig;

    @Autowired
    private RedisService redisService;

    @Autowired
    private StreamService streamService;

    @Autowired
    private S3Service s3Service;

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
    public void download(S3 s3, HttpServletRequest request, HttpServletResponse response) throws IOException {
        s3Service.initClient(s3, "");
        downloadStream(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getOffset(), request, response);
    }

    @Override
    public void delete(S3 s3) {
        s3Service.initClient(s3, "");
        s3Service.deleteObjects(s3.getClientId(), s3.getBucket(), s3.getKey());
    }

    @Override
    public List<ListObjectResult> list(S3 s3) {
        s3Service.initClient(s3, "");
        List<ListObjectResult> objects = new ArrayList<>();
        ListObjectsV2Iterable listResp = null;
        try {
            listResp = s3Service.listObject(s3.getClientId(), s3.getBucket(), "");
            listResp.contents().stream()
                .forEach(content -> objects.add(new ListObjectResult(content.key(),
                        content.lastModified(),
                        content.size())));
        } catch (NoSuchBucketException e) {
            if (log.isWarnEnabled()) {
                log.warn("bucket not exist");
            }
            if(s3Service.createBucket(s3.getClientId(), s3.getPlatform(), s3.getBucket())) {
                return objects;
            }
        }
        return objects;
    }

    @Override
    public String getObjectUrl(S3 s3) throws IOException {
        s3Service.initClient(s3, "");
        String plainText = s3.getPlatform() + "*" + s3.getRegion() + "*" + s3.getBucket();
        byte[] iv = BytesUtils.generateRandomByte(16);
        byte[] ciphertext = EncryptUtils.encrypt(plainText, iv);
        String cipherBase64 = Base64.getUrlEncoder().encodeToString(ciphertext);
        String ivBase64 = Base64.getUrlEncoder().encodeToString(iv);
        String codeVerifier = PkceUtils.generateVerifier();
        String codeChallenge = PkceUtils.generateChallenge(codeVerifier);
        redisService.set(s3.getKey() + "-challenge", codeChallenge, serviceconfig.getTimeout().getStorageUrl());
        redisService.set(s3.getKey() + "-secret", ivBase64, serviceconfig.getTimeout().getStorageUrl());
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
        return "0";    }

    @Override
    public String obtainUploadId(S3 s3) throws IOException {
        s3Service.initClient(s3, "");
        String match = streamService.checkSha256(s3);
        if (log.isDebugEnabled()) {
            log.debug("sha256 match result:{}", match);
        }
        if (match != null) {
            return match;
        }
        if (!"minio".equals(s3.getPlatform())) {
            List<MultipartUpload> uploads = s3Service.listMultipartUploads(s3.getClientId(), s3.getBucket()).uploads();
            for (MultipartUpload upload : uploads) {
                if (s3.getKey().equals(upload.key())) {
                    return upload.uploadId();
                }
            }
        }
        return s3Service.createMultiPart(s3.getClientId(), s3.getBucket(), s3.getKey()).uploadId();
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
        if (fileSize < partSize && StringUtils.isEmpty(s3.getPartNum())) {
            String eTag = s3Service.uploadObject(s3.getClientId(), s3.getBucket(), s3.getKey(), requestBody);
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
                String uploadId = s3Service.getObject(s3.getClientId(), s3.getBucket(), s3.getConsumerGroupId() + "-uploadId");
                if (log.isInfoEnabled()) {
                    log.info("uploadPart consumerGroupId:{}, uploadId:{}", s3.getConsumerGroupId(), uploadId);
                }
                if (StringUtils.isNotBlank(uploadId)) {
                    s3.setUploadId(uploadId);
                }
            }
            s3Service.listCompletedPart(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
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
                    String sha = s3Service.getObject(s3.getClientId(), s3.getBucket(), shaKey);
                    if (sha256.equals(sha)) {
                        UploadPartResponse uploadPartResponse = s3Service.uploadPart(s3.getClientId(), s3, requestBody, partNum, endOffset);
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
                            log.info("uploadPart sha256:{} not match with s3:{}, key:{}", sha256, sha, shaKey);
                        }
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("uploadPart, part:{} is complete, skip", partNum);
                    }
                }
            }
            if (StringUtils.isBlank(s3.getPartCount()) && completedParts.size() == num) {
                s3Service.completePart(s3.getClientId(), s3.getBucket(), s3.getKey(), s3.getUploadId(), completedParts);
            }
        }
        return completedParts;
    }

    private void downloadStream(String clientId, String bucket, String key, String offset, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String range = request.getHeader("Range");
        if (StringUtils.isNotBlank(range)) {
            String[] ranges = range.replace("bytes=", "").split("-");
            offset = ranges[0];
        }
        ResponseInputStream<GetObjectResponse> in =
                s3Service.getObjectStream(clientId, bucket, key, offset);
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
}
