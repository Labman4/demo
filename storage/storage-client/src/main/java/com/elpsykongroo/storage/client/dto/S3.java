package com.elpsykongroo.storage.client.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class S3 {
    String accessKey;
    String accessSecret;
    String region;
    String endpoint;
    String bucket;
    String key;
    MultipartFile data[];

    byte[] bytedata;
    String partSize = "5242880";

    String offset;

    /*
    use for sts
     */
    String idToken;
}

