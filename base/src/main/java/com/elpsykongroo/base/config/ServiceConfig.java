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

package com.elpsykongroo.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "service")
public class ServiceConfig {
    private String initAdminAuth;

    private TimeOut timeout;

    private String env;

    private String whiteDomain;

    private Url url;

    private String adminEmail;

    private String vault;

    private Elastic es;
    //public ca
    private SSL ssl;

    private String proxy;

    private S3 s3;

    @Data
    public static class SSL {
        
        private String type;

        private String ca ;

        private String cert;

        private String key;
    }

    @Data
    public static class Elastic {

        private TimeOut timeout;
        //self ca
        private SSL ssl ;

        private String[] nodes;

    }

    @Data
    public static class S3 {

        private String accessKey;

        private String accessSecret ;

        private String endpoint;

        private String region;


    }

    @Data
    public static class TimeOut {

        private Long connect;

        private Long socket;

        private Long read;
    }

    @Data
    public static class Url {
        private String es;
        private String redis;
        private String storage;
        private String auth;
    }
}