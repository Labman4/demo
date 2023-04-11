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

package com.elpsykongroo.services.elasticsearch.server.service.impl;

import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecord;
import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecordDto;
import com.elpsykongroo.services.elasticsearch.server.repo.AccessRecordRepo;
import com.elpsykongroo.services.elasticsearch.server.service.AccessRecordService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessRecordServiceImpl implements AccessRecordService {


    @Autowired
    private AccessRecordRepo accessRecordRepo;

    @Autowired
    public AccessRecordServiceImpl(AccessRecordRepo accessRecordRepo) {
        this.accessRecordRepo = accessRecordRepo;
    }

    @Override
    public Page<AccessRecord> findAll(Pageable pageable) {
        return accessRecordRepo.findAll(pageable);
    }

    @Override
    public List<AccessRecord> findByAccessPathLike(String path) {
        return accessRecordRepo.findByAccessPathLike(path);
    }

    @Override
    public List<AccessRecord> searchSimilar(AccessRecordDto accessRecord) {
        StringBuffer field = new StringBuffer();
        if (StringUtils.isNotBlank(accessRecord.getAccessPath())) {
            field.append(accessRecord.getAccessPath()).append(",");
        }
        if (StringUtils.isNotBlank(accessRecord.getSourceIP())) {
            field.append(accessRecord.getSourceIP()).append(",");
        }
        if (StringUtils.isNotBlank(accessRecord.getUserAgent())) {
            field.append(accessRecord.getUserAgent()).append(",");
        }
        if (accessRecord.getRequestHeader() != null && accessRecord.getRequestHeader().size() > 0) {
            field.append(JsonUtils.toJson(accessRecord.getRequestHeader())).append(",");
        }
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        if ("1".equals(accessRecord.getOrder())) {
            sort = Sort.by(Sort.Direction.ASC, "timestamp");
        }
        String[] fields = field.toString().substring(0, field.length() - 1).split(",");
        Pageable pageable = PageRequest.of(Integer.parseInt(accessRecord.getPageNum()), Integer.parseInt(accessRecord.getPageSize()), sort);
        AccessRecord record = new AccessRecord();
        record.setAccessPath(accessRecord.getAccessPath());
        record.setUserAgent(accessRecord.getUserAgent());
        record.setRequestHeader(accessRecord.getRequestHeader());
        record.setSourceIP(accessRecord.getSourceIP());
        record.setTimestamp(accessRecord.getTimestamp());
        accessRecordRepo.save(record);
        List<AccessRecord> records = accessRecordRepo.searchSimilar(record, fields, pageable).get().toList();
        accessRecordRepo.deleteById(record.getId());
        return records;
    }

    @Override
    public List<AccessRecord> findBySourceIP(String sourceip) {
        return accessRecordRepo.findBySourceIP(sourceip);
    }

    @Override
    public List<AccessRecord> findByUserAgentLike(String agent) {
        return accessRecordRepo.findByUserAgentLike(agent);
    }

    @Override
    public List<AccessRecord> findByRequestHeaderLike(String header) {
        return accessRecordRepo.findByRequestHeaderLike(header);
    }

    @Override
    public void deleteAllById(List<String> ids) {
        accessRecordRepo.deleteAllById(ids);
    }

    @Override
    public String save(AccessRecord accessRecord) {
        return accessRecordRepo.save(accessRecord).getId();
    }
}
