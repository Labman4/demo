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

package com.elpsykongroo.services.elasticsearch.client;

import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecord;
import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecordDto;
import com.elpsykongroo.services.elasticsearch.client.dto.IPManage;

import java.util.List;

public interface SearchService {
    List<AccessRecord> searchSimilar(AccessRecordDto accessRecord);

    List<IPManage> findByIsBlackTrue();

    List<IPManage> findByIsBlackFalse();

    String findByIsBlackTrue(String pageNumber, String pageSize);

    String findByIsBlackFalse(String pageNumber, String pageSize);

    Long countByAddressAndIsBlackTrue(String address);

    Long countByAddressAndIsBlackFalse(String address);

    void deleteByAddressAndIsBlackTrue(String address);

    void deleteByAddressAndIsBlackFalse(String address);

    String findAllIP(String pageNumber, String pageSize);

    String findAllRecord(String pageNumber, String pageSize, String order);

    void saveRecord(AccessRecord accessRecord);

    String saveIP(IPManage IPManage);

    List<AccessRecord> findByAccessPathLike(String path);

    List<AccessRecord> findBySourceIP(String sourceip);

    List<AccessRecord> findByUserAgentLike(String agent);

    List<AccessRecord> findByRequestHeaderLike(String header);

    void deleteAllRecordById(List<String> ids);

    void deleteIPById(String id);

}
