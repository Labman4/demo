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

package com.elpsykongroo.services.elasticsearch.service;

import com.elpsykongroo.services.elasticsearch.domain.AccessRecord;

import java.util.List;

public interface AccessRecordService {
    List<AccessRecord> findAll(String pageNum, String pageSize, String order);

    List<AccessRecord> findBySourceIP(String ip);

    List<AccessRecord> filter(String param, String pageNo, String pageSize, String order);

    void deleteAllById(Iterable<String> ids);

    String save(AccessRecord accessRecord);
}
