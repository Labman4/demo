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
    private TimeOut timeout;
    private String env;
    private String whiteDomain;
    private Url es;
    private Url storage;
    private Url auth;
    private Url redis;
    private String adminEmail;
    private String vault;
    private ES elastic;
    //public ca
    private SSL ssl;

    @Data
    public static class SSL {
        
        private String type;

        private String ca ;

        private String cert;

        private String key;
    }

    @Data
    public static class ES {

        private TimeOut timeout;

        private String user;

        private String pass;

        //self ca
        private SSL ssl ;

        private String[] nodes;

    }

    @Data
    public static class TimeOut {

        private Long connect;

        private Long socket;

        private Long read;
    }

    @Data
    public static class Url {

        private String url;
    }
}