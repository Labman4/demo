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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.elpsykongroo.base.domain.search.QueryParam;
import com.elpsykongroo.base.utils.IPUtils;
import com.elpsykongroo.base.domain.search.repo.AccessRecord;
import com.elpsykongroo.base.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;

import com.elpsykongroo.gateway.config.RequestConfig;
import com.elpsykongroo.gateway.config.RequestConfig.Record.Exclude;
import com.elpsykongroo.gateway.service.AccessRecordService;
import com.elpsykongroo.gateway.service.IPManagerService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccessRecordServiceImpl implements AccessRecordService {

	@Autowired
	private SearchService searchService;

	@Autowired
	private IPManagerService ipMangerService;

	@Autowired
	private RequestConfig requestConfig;

	public AccessRecordServiceImpl(IPManagerService ipMangerService, RequestConfig requestConfig) {
		this.ipMangerService = ipMangerService;
		this.requestConfig = requestConfig;
	}

	@Override
	public void saveAccessRecord(HttpServletRequest request) {
		String ip = ipMangerService.accessIP(request, "record");
		Exclude recordExclude = requestConfig.getRecord().getExclude();
		if (log.isTraceEnabled()) {
			log.trace("record exclude:{}", recordExclude);
		}
		boolean recordFlag = ipMangerService.filterByIpOrList(recordExclude.getIp(), ip);
		if (!(StringUtils.isNotEmpty(recordExclude.getPath())
				&& beginWithPath(recordExclude.getPath(), request.getRequestURI()))) {
			if (!recordFlag) {
				Map<String, String> result = new HashMap<>();
				Enumeration<String> headerNames = request.getHeaderNames();
				while (headerNames.hasMoreElements()) {
					String key = headerNames.nextElement();
					String value = request.getHeader(key);
					result.put(key, value);
				}
				ip = ipMangerService.accessIP(request, "ip");
				AccessRecord record = new AccessRecord();
				record.setRequestHeader(result);
				record.setAccessPath(request.getRequestURI());
				record.setSourceIP(ip);
				record.setTimestamp(Instant.now().toString());
				record.setUserAgent(request.getHeader("user-agent"));
				saveRecord(record);
				if (log.isDebugEnabled()) {
					log.debug("request header------------{} ", result);
				}
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
	public String findAll(String pageNo, String pageSize, String order) {
		QueryParam queryParam = new QueryParam();
		queryParam.setOrder(order);
		queryParam.setOrderBy("timestamp");
		queryParam.setPageNumber(pageNo);
		queryParam.setPageSize(pageSize);
		queryParam.setIndex("access_record");
		queryParam.setType(AccessRecord.class);
		return searchService.query(queryParam);
	}

	@Override
	public String deleteRecord(String params) throws UnknownHostException {
		if (StringUtils.isBlank(params)) {
			return "0";
		}
		QueryParam deleteParam = new QueryParam();
		deleteParam.setOperation("delete");
		deleteParam.setIndex("access_record");
		if (!params.contains(",")) {
			if (IPUtils.validateHost(params) || IPUtils.validateHost(params)) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(params);
				String records = "";
				QueryParam queryParam = new QueryParam();
				queryParam.setIndex("access_record");
				queryParam.setType(AccessRecord.class);
				queryParam.setField("sourceIP");
				queryParam.setFuzzy(false);
				for (InetAddress addr: inetAddresses) {
					queryParam.setParam(addr.getHostAddress());
					records += searchService.query(queryParam);
				}
				deleteParam.setIds(records);
				return searchService.query(deleteParam);
			}
		}
		deleteParam.setIds(params);
		return searchService.query(deleteParam);
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
			return findAll(pageNo, pageSize, order);
		}
	}

	private boolean beginWithPath(String paths, String url) {
		String[] path = paths.split(",");
		for (String p: path) {
			if (url.startsWith(p)) {
				return true;
			}
		}
	return false;
}
}
