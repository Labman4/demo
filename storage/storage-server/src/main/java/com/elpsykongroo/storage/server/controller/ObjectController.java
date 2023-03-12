package com.elpsykongroo.storage.server.controller;


import com.elpsykongroo.storage.server.entity.S3;
import com.elpsykongroo.storage.server.service.ObjectService;
import com.elpsykongroo.storage.server.utils.JsonUtils;
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
@RequestMapping("/storage/object")
public class ObjectController {
    @Autowired
    private ObjectService objectService;

    @PutMapping("upload")
    public void multipartUpload(@ModelAttribute S3 s3) {
        try {
            objectService.multipartUpload(s3);
        } catch (Exception e) {
            log.error("multipart error: ", e);
        }
    }

    @PostMapping("download")
    public void download(S3 s3, HttpServletResponse response) {
        try {
            objectService.download(s3, response);
        } catch (IOException e) {
            log.info("download error");
        }
    }

    @PostMapping("list")
    public String list (@RequestBody S3 s3) {
        return JsonUtils.toJson(objectService.list(s3));
    }

    @PostMapping("delete")
    public void delete (@RequestBody S3 s3) { objectService.delete(s3); }
}
