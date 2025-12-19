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

package com.elpsykongroo.gateway.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.elpsykongroo.base.domain.search.QueryParam;
import com.elpsykongroo.base.utils.IPUtils;
import com.elpsykongroo.base.domain.search.repo.AccessRecord;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.service.SearchService;
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.base.utils.RecordUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.servlet.http.HttpServletRequest;

import com.elpsykongroo.base.config.RequestConfig;
import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.gateway.service.AccessRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.client.VaultEndpoint;

@Service
@Slf4j
public class AccessRecordServiceImpl implements AccessRecordService {

	@Autowired
	private SearchService searchService;

	@Autowired
	private RedisService redisService;

	@Autowired
	private RequestConfig requestConfig;
	
	@Autowired
	private ServiceConfig serviceConfig;

	@Autowired
	private VaultEndpoint vaultEndpoint;

	@Autowired
    private ClientAuthentication clientAuthentication; 

	public AccessRecordServiceImpl(RequestConfig requestConfig) {
		this.requestConfig = requestConfig;
	}

	@Override
	public void saveAccessRecord(HttpServletRequest request) {
		RecordUtils recordUtils = new RecordUtils(redisService, requestConfig, vaultEndpoint, clientAuthentication, serviceConfig.getRecordExcludeIpPath(), serviceConfig.getRecordExcludeIpKey());
		if (recordUtils.filterRecord(request)) {
			Map<String, String> result = new HashMap<>();
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String key = headerNames.nextElement();
				String value = request.getHeader(key);
				result.put(key, value);
			}
			IPUtils ipUtils = new IPUtils(requestConfig);
			AccessRecord record = new AccessRecord();
			record.setRequestHeader(result);
			record.setAccessPath(request.getRequestURI());
			record.setSourceIP(ipUtils.accessIP(request, ""));
			record.setTimestamp(Instant.now().toString());
			record.setUserAgent(request.getHeader("user-agent"));
			saveRecord(record);
			if (log.isDebugEnabled()) {
				log.debug("request header------------{} ", result);
			}
		}
	}

	@Override
	public void saveRecord(AccessRecord record) {
		try {
			QueryParam queryParam = new QueryParam();
			queryParam.setIndex("access_record");
			queryParam.setOperation("save");
			queryParam.setEntity(record);
			searchService.query(queryParam);
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("saveRecord error: {}", e.getMessage());
			}
		}
	}

	@Override
	public String findAll(String pageNo, String pageSize, String order, String id) {
		if (StringUtils.isBlank(id)) {
			id = "1";
		}
		QueryParam queryParam = new QueryParam();
		queryParam.setOrder(order);
		queryParam.setOrderBy("timestamp");
		queryParam.setPageNumber(pageNo);
		queryParam.setPageSize(pageSize);
		queryParam.setIndex("access_record");
		queryParam.setType(AccessRecord.class);
		queryParam.setScrollId(id);
		return searchService.query(queryParam);
	}

	@Override
	public String deleteRecord(List<String> params) throws UnknownHostException {
		if (params.isEmpty()) {
			return "0";
		}
		List<String> ids = new ArrayList<>();
		QueryParam deleteParam = new QueryParam();
		deleteParam.setOperation("deleteQuery");
		deleteParam.setIndex("access_record");
		for (String param : params) {
			if (IPUtils.validateHost(param) || IPUtils.validate(param)) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(param);
				QueryParam queryParam = new QueryParam();
				queryParam.setIndex("access_record");
				queryParam.setType(AccessRecord.class);
				queryParam.setFields(Collections.singletonList("sourceIP"));
				queryParam.setBoolQuery(true);
				if (param.contains("::") && IPUtils.isIpv6(param)) {
					queryParam.setQueryStringParam(Collections.singletonList("\"" + param + "\""));
					addResult(ids, queryParam);
				} else {
					for (InetAddress addr : inetAddresses) {
						if (IPUtils.isIpv6(addr.getHostAddress())) {
							queryParam.setQueryStringParam(Collections.singletonList("\"" + addr.getHostAddress() + "\""));
						} else {
							queryParam.setQueryStringParam(Collections.singletonList(addr.getHostAddress()));
						}
						addResult(ids, queryParam);
					}
				}
			} else {
				ids.add(param);
			}
		}
		deleteParam.setIds(ids);
		deleteParam.setIdsQuery(true);
		return searchService.query(deleteParam);
	}

	private void addResult(List<String> ids, QueryParam queryParam) {
		String result = searchService.query(queryParam);
		if (StringUtils.isNotEmpty(result)) {
			List<String> idList = JsonUtils.toType(result, new TypeReference<>() {
			});
			if (log.isDebugEnabled()) {
				log.debug("delete query string result: {}", idList.size());
			}
			ids.addAll(idList);
		}
	}


	@Override
	public String filterByParams(String params, String pageNo, String pageSize, String order){
		if (StringUtils.isNotBlank(params)) {
			List<String> fields = new ArrayList<>();
			fields.add("sourceIP");
			fields.add("userAgent");
			fields.add("accessPath");
			fields.add("requestHeader");
			QueryParam queryParam = new QueryParam();
			queryParam.setOrder(order);
			queryParam.setOrderBy("timestamp");
			queryParam.setPageNumber(pageNo);
			queryParam.setPageSize(pageSize);
			queryParam.setIndex("access_record");
			queryParam.setType(AccessRecord.class);
			queryParam.setParam(params);
			queryParam.setFields(fields);
			queryParam.setFuzzy(true);
			return searchService.query(queryParam);
		} else {
			return findAll(pageNo, pageSize, order, "1");
		}
	}
}
