package com.elpsykongroo.storage.client;

import com.elpsykongroo.storage.client.dto.S3;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface StorageService {
    void uploadObject(S3 s3) throws IOException;

    void downloadObject(S3 s3, HttpServletResponse response) throws IOException;

    void deleteObject(S3 s3);

    String listObject(S3 s3);
}
