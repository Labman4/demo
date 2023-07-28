/*
 * Copyright 2020-2022 the original author or authors.
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

package com.elpsykongroo.base.utils;

import com.elpsykongroo.base.domain.search.repo.AccessRecord;
import com.elpsykongroo.base.service.GatewayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RecordUtils {

    private GatewayService gatewayService;

    public RecordUtils(GatewayService gatewayService) {
        this.gatewayService = gatewayService;
    }
    public void saveRecord(HttpServletRequest request, String ip) {
        try {
            Map<String, String> result = new HashMap<>();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                String value = request.getHeader(key);
                result.put(key, value);
            }
            AccessRecord record = new AccessRecord();
            record.setSourceIP(ip);
            record.setRequestHeader(result);
            record.setAccessPath(request.getRequestURI());
            record.setTimestamp(Instant.now().toString());
            record.setUserAgent(request.getHeader("user-agent"));
            gatewayService.saveRecord(record);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("saveRecord error:{}", e.getMessage());
            }
        }
    }
}
