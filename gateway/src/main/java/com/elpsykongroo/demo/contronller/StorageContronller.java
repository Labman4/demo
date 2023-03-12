package com.elpsykongroo.demo.contronller;


import com.elpsykongroo.storage.client.StorageService;
import com.elpsykongroo.storage.client.dto.S3;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("storage")
public class StorageContronller {
    @Autowired
    private StorageService storageService;

    @PutMapping("/object/upload")
    public void uploadObject(@ModelAttribute S3 s3) {
        try {
            storageService.uploadObject(s3);
        } catch (Exception e) {
            log.error("object upload multipart error: ", e);
        }
    }

    @PostMapping("/object/download")
    public void download(S3 s3, HttpServletResponse response) {
        try {
            storageService.downloadObject(s3, response);
        } catch (IOException e) {
            log.error("object download error: ", e);
        }
    }

    @PostMapping("/object/list")
    public String list (@RequestBody S3 s3) {
        return storageService.listObject(s3);
    }

    @PostMapping("/object/delete")
    public void delete (@RequestBody S3 s3) { storageService.deleteObject(s3); }
}
