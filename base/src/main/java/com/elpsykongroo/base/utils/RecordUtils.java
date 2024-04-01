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
import com.elpsykongroo.base.service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;

import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;

import java.time.Instant;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class RecordUtils {

    private GatewayService gatewayService;

    private RedisService redisService;

    private RequestConfig requestConfig;

    private VaultEndpoint vaultEndpoint;

    private ClientAuthentication clientAuthentication;

    private String secretPath;

    private String secretKey;

    public RecordUtils(GatewayService gatewayService, RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
        this.gatewayService = gatewayService;
    }

    public RecordUtils(GatewayService gatewayService, RedisService redisService, RequestConfig requestConfig, VaultEndpoint vaultEndpoint, ClientAuthentication clientAuthentication, String secretPath, String secretKey) {
        this.requestConfig = requestConfig;
        this.gatewayService = gatewayService;
        this.vaultEndpoint = vaultEndpoint;
        this.clientAuthentication = clientAuthentication;
        this.secretKey = secretKey;
        this.secretPath = secretPath;
        this.redisService = redisService;
    }

    public RecordUtils(RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    public RecordUtils(RedisService redisService, RequestConfig requestConfig, VaultEndpoint vaultEndpoint, ClientAuthentication clientAuthentication, String secretPath, String secretKey) {
        this.requestConfig = requestConfig;
        this.vaultEndpoint = vaultEndpoint;
        this.clientAuthentication = clientAuthentication;
        this.secretKey = secretKey;
        this.secretPath = secretPath;
        this.redisService = redisService;
    }

    public boolean filterRecord(HttpServletRequest request) {
            IPUtils ipUtils = new IPUtils(requestConfig);
            String ip = ipUtils.accessIP(request, "record");
            RequestConfig.Record.Exclude recordExclude = requestConfig.getRecord().getExclude();
            String excludeIp = "";
            if (vaultEndpoint != null && clientAuthentication != null) {
                try {
                    excludeIp = redisService.get(secretKey);
                } catch (FeignException e) {
                    log.error("get cache failed, skip");
                }
                if (StringUtils.isBlank(excludeIp)) {
                    VaultTemplate vaultTemplate = new VaultTemplate(vaultEndpoint, clientAuthentication);
                    VaultResponse response = vaultTemplate.read(secretPath);
                    JsonNode jsonNode = JsonUtils.toJsonNode(JsonUtils.toJson(response.getData().get("data")));
                    excludeIp = jsonNode.get(secretKey).asText();
                    log.info("RecordUtils exclude ip:{}", excludeIp);
                    try {
                        redisService.set(secretKey, excludeIp, "60");
                    } catch (FeignException e) {
                        log.error("set cache failed, skip");
                    }
                }  
            } else {
                excludeIp = recordExclude.getIp();
            }
            if (log.isTraceEnabled()) {
                log.trace("RecordUtils exclude:{}", recordExclude);
            }
            boolean recordFlag = IPUtils.filterByIpOrList(excludeIp, ip);
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