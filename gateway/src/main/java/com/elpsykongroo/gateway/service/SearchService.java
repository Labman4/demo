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

package com.elpsykongroo.gateway.service;

import com.elpsykongroo.gateway.entity.AccessRecord;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;

public interface SearchService {
    @RequestLine("PUT /search/record")
    void save(@QueryMap AccessRecord accessRecord);

    @RequestLine("GET /search/record?pageNumber={pageNumber}&pageSize={pageSize}&order={order}")
    String recordList(@Param("pageNumber") String pageNumber,
                   @Param("pageSize") String pageSize,
                   @Param("order") String order);

    @RequestLine("POST /search/record")
    String filter(@Param("params") String params,
                  @Param("pageNumber") String pageNumber,
                  @Param("pageSize") String pageSize,
                  @Param("order") String order);

    @RequestLine("DELETE /search/record/{ids}")
    void deleteRecord(@Param("ids") String ids);

    @RequestLine("GET /search/record/ip?ip={ip}")
    String findByIP(@Param("ip") String ip);

    @RequestLine("PUT /search/ip?address={address}&black={black}")
    String saveIP(@Param String address, @Param String black);

    @RequestLine("GET /search/ip/page?pageNumber={pageNumber}&pageSize={pageSize}&order={order}&black={black}")
    String ipPageList(@Param String pageNumber, @Param String pageSize, @Param("black") String black);

    @RequestLine("GET /search/ip?black={black}")
    String ipList(@Param("black") String black);

    @RequestLine("GET /search/ip/count?address={address}&black={black}")
    String ipCount(@Param("address") String address, @Param("black") String black);

    @RequestLine("DELETE /search/ip/black/{address}")
    String deleteBlack(@Param String address);

    @RequestLine("DELETE /search/ip/white/{address}")
    String deleteWhite(@Param String address);

    @RequestLine("DELETE /search/ip/{id}")
    String deleteIpById(@Param String id);
}
