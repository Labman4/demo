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
        private String ip;

        private String black;

        private String white;

        private String record;

    }

    @Data
    public static class Token {
        private Long tokens;

        private Long duration;

        private Long speed;
    }
   
    @Data
    public static class Path {
        private String permit;

        private String nonPrivate;
    
        private String limit;
        
        private String exclude;
    
        private String filter;
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
