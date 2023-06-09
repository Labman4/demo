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

package com.elpsykongroo.services.elasticsearch.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import com.elpsykongroo.services.elasticsearch.domain.AccessRecord;
import com.elpsykongroo.services.elasticsearch.repo.AccessRecordRepo;
import com.elpsykongroo.services.elasticsearch.service.AccessRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AccessRecordServiceImpl implements AccessRecordService {

    @Autowired
    private AccessRecordRepo accessRecordRepo;

    @Autowired
    private ElasticsearchOperations operations;

    @Autowired
    public AccessRecordServiceImpl(AccessRecordRepo accessRecordRepo) {
        this.accessRecordRepo = accessRecordRepo;
    }

    @Override
    public List<AccessRecord> findAll(String pageNum, String pageSize, String order) {
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        if ("1".equals(order)) {
            sort = Sort.by(Sort.Direction.ASC, "timestamp");
        }
        Pageable pageable = PageRequest.of(Integer.parseInt(pageNum), Integer.parseInt(pageSize), sort);
        return accessRecordRepo.findAll(pageable).get().toList();
    }

    @Override
    public List<AccessRecord> filter(String param, String pageNum, String pageSize, String order) {
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        if ("1".equals(order)) {
            sort = Sort.by(Sort.Direction.ASC, "timestamp");
        }
        Pageable pageable = PageRequest.of(Integer.parseInt(pageNum), Integer.parseInt(pageSize), sort);
        List<String>  fields = new ArrayList<>();
        fields.add("sourceIP");
        fields.add("userAgent");
        fields.add("accessPath");
        fields.add("requestHeader");
        MultiMatchQuery multiMatchQuery = new MultiMatchQuery.Builder()
                .query(param)
                .fields(fields)
                .fuzziness("auto")
                .build();
        Query query = NativeQuery.builder().withQuery(q ->
                q.multiMatch(multiMatchQuery)

        ).withPageable(pageable).build();
        SearchHits<AccessRecord> searchHits = operations.search(query, AccessRecord.class, IndexCoordinates.of("access_record"));
        SearchPage<AccessRecord> searchPage = SearchHitSupport.searchPageFor(searchHits, pageable);
        Page<AccessRecord> page = (Page<AccessRecord>) SearchHitSupport.unwrapSearchHits(searchPage);
        return page.get().toList();

    }

    @Override
    public List<AccessRecord> findBySourceIP(String sourceip) {
        return accessRecordRepo.findBySourceIP(sourceip);
    }

    @Override
    public void deleteAllById(String ids) {
        List<String> deleteIds = Arrays.stream(ids.split(",")).collect(Collectors.toList());
        accessRecordRepo.deleteAllById(deleteIds);
        log.debug("delete record size:{}", deleteIds.size());
    }

    @Override
    public String save(AccessRecord accessRecord) {
        return accessRecordRepo.save(accessRecord).getId();
    }
}
