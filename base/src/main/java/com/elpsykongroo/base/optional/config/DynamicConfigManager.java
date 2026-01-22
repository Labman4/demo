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

package com.elpsykongroo.base.optional.config;

import com.elpsykongroo.base.config.DynamicConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.Versioned;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@EnableScheduling
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "service",
        name = "vault",
        havingValue = "true",
        matchIfMissing = false)
public class DynamicConfigManager {

    private final VaultTemplate vaultTemplate;

    private final AtomicReference<DynamicConfig> ref = new AtomicReference<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile int lastVersion = -1;

    public DynamicConfigManager(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    @Value("${SECRETS_APP_DYNAMIC_PATH:app/dynamic}")
    private String path;

    @Scheduled(fixedDelayString = "${VAULT_REFRESH_DURATION:30000}")
    public void refreshIfNeeded() {
        Versioned<Map<String, Object>> kv = vaultTemplate.opsForVersionedKeyValue("kv").get(path);
        if (kv == null) return;
        int version = kv.getMetadata().getVersion().getVersion();
        if (version != lastVersion) {
            ref.set(mapToConfig(kv.getData()));
            log.info("DynamicConfig refreshed to version {}",  version);
            lastVersion = version;
        }
    }

    public DynamicConfig get() {
        return ref.get();
    }

    private DynamicConfig mapToConfig(Map<String, Object> data) {
        return objectMapper.convertValue(data, DynamicConfig.class);
    }
}