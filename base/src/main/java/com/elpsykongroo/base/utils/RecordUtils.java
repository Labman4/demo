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

import com.elpsykongroo.base.config.RequestConfig;
import com.elpsykongroo.base.domain.search.repo.AccessRecord;
import com.elpsykongroo.base.service.GatewayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RecordUtils {

    private GatewayService gatewayService;

    private RequestConfig requestConfig;

    public RecordUtils(GatewayService gatewayService, RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
        this.gatewayService = gatewayService;
    }

    public RecordUtils(RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    public boolean filterRecord(HttpServletRequest request) {
            IPUtils ipUtils = new IPUtils(requestConfig);
            String ip = ipUtils.accessIP(request, "record");
            RequestConfig.Record.Exclude recordExclude = requestConfig.getRecord().getExclude();
            if (log.isTraceEnabled()) {
                log.trace("RecordUtils exclude:{}", recordExclude);
            }
            boolean recordFlag = IPUtils.filterByIpOrList(recordExclude.getIp(), ip);
            if (!(StringUtils.isNotEmpty(recordExclude.getPath())
                    && PathUtils.beginWithPath(recordExclude.getPath(), request.getRequestURI()))) {
                if (!recordFlag) {
                    return true;
                }
            }
        return false;
    }

    public void saveRecord(HttpServletRequest request) {
        IPUtils ipUtils = new IPUtils(requestConfig);
        Map<String, String> result = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            String value = request.getHeader(key);
            result.put(key, value);
        }
        AccessRecord record = new AccessRecord();
        record.setSourceIP(ipUtils.accessIP(request, ""));
        record.setRequestHeader(result);
        record.setAccessPath(request.getRequestURI());
        record.setTimestamp(Instant.now().toString());
        record.setUserAgent(request.getHeader("user-agent"));
        try {
            gatewayService.saveRecord(record);
            if (log.isDebugEnabled()) {
                log.debug("request header------------{} ", result);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("RecordUtils saveRecord error:{}", e.getMessage());
            }
        }
    }
}