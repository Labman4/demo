package com.elpsykongroo.storage.client;

import jakarta.servlet.http.HttpServletResponse;
import com.elpsykongroo.base.domain.storage.object.S3;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface StorageService {
    ResponseEntity uploadObject(S3 s3);

    void downloadObject(S3 s3, HttpServletResponse response) throws IOException;

    void deleteObject(S3 s3);

    String listObject(S3 s3);
}
