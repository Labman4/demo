package com.elpsykongroo.storage.server.service;

import com.elpsykongroo.storage.server.entity.S3;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
public interface ObjectService {

    void download(S3 s3, HttpServletResponse response) throws IOException;

    void delete(S3 s3);

    Map<String, Long> list(S3 s3);

    void multipartUpload(S3 s3) throws Exception;
}
