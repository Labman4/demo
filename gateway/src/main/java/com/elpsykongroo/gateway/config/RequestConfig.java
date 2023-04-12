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

package com.elpsykongroo.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Component
@ConfigurationProperties(prefix = "request")
@Data
public class RequestConfig {
    private Path path;

    private Limit limit;

    private Header header;

    private Record record;

    @Data
    public static class Limit  {
        private Token global;

        private Token scope;
    }     

    @Data
    public static class Header {
        private String ip = "X-Forwarded-For";

        private String black = "X-Forwarded-For";

        private String white = "X-Forwarded-For";

        private String record = "X-Forwarded-For";

    }

    @Data
    public static class Token {
        private Long tokens = 1000L;

        private Long duration = 1L;

        private Long speed = 100L;
    }
   
    @Data
    public static class Path {
        private String permit = "/**";

        private String nonPrivate = "/public";
    
        private String limit = "/";
        
        private String exclude;
    
        private String filter = "/";  
    }

    @Data
    public static class Record {
        private Exclude exclude;

        @Data
        public static class Exclude {
            private String path;
    
            private String ip;
    
        }
    }

}
