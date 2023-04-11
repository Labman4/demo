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

package com.elpsykongroo.services.elasticsearch.server.service;

import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecord;
import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecordDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AccessRecordService {
    Page<AccessRecord> findAll(Pageable pageable);

    List<AccessRecord> findByAccessPathLike(String path);

    List<AccessRecord> findBySourceIP(String ip);

    List<AccessRecord> searchSimilar(AccessRecordDto accessRecord);

    List<AccessRecord> findByUserAgentLike(String agent);

    List<AccessRecord> findByRequestHeaderLike(String header);

    void deleteAllById(List<String> ids);

    String save(AccessRecord accessRecord);
}
