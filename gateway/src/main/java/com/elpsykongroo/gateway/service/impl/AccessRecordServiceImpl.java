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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.elpsykongroo.base.utils.IPRegexUtils;
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.base.domain.AccessRecord;
import com.elpsykongroo.base.service.SearchService;
import com.fasterxml.jackson.core.type.TypeReference;
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

	public void saveAccessRecord(HttpServletRequest request) {
		try {
			String ip = ipMangerService.accessIP(request, "record");
			Exclude recordExclude = requestConfig.getRecord().getExclude();
			boolean recordFlag = ipMangerService.filterByIpOrList(recordExclude.getIp(), ip);
			if (!(StringUtils.isNotEmpty(recordExclude.getPath()) && beginWithPath(recordExclude.getPath(), request.getRequestURI()))) {
				if (!recordFlag) {
					Map<String, String> result = new HashMap<>();
					Enumeration<String> headerNames = request.getHeaderNames();
					while (headerNames.hasMoreElements()) {
						String key = (String) headerNames.nextElement();
						String value = request.getHeader(key);
						result.put(key, value);
					}
					ip = ipMangerService.accessIP(request, "ip");
					AccessRecord record = new AccessRecord();
					record.setRequestHeader(result);
					record.setAccessPath(request.getRequestURI());
					record.setSourceIP(ip);
					record.setTimestamp(new Date());
					record.setUserAgent(request.getHeader("user-agent"));
					searchService.save(record);
					log.debug("request header------------{} ", result);
			    }
		  	}
		} catch (Exception e) {
			log.error("save record error: {}", e.getMessage());
		}
	}

	@Override
	public String findAll(String pageNo, String pageSize, String order) {
		return searchService.recordList(pageNo, pageSize ,order);
	}

	@Override
	public int deleteRecord(String params) throws UnknownHostException {
		List<String> recordIds = new ArrayList<>();
		if (StringUtils.isNotEmpty(params)) {
			if (IPRegexUtils.vaildate(params)) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(params);
				for (InetAddress addr: inetAddresses) {
					List<AccessRecord> accessRecord = JsonUtils.toType(searchService.findByIP(addr.getHostAddress()), new TypeReference<List<AccessRecord>>() {});
					for (AccessRecord record: accessRecord) {
						recordIds.add(record.getId());
					}
				}
			} else {
				recordIds = new ArrayList<String>(Arrays.asList(params.split(",")));
			}
		}
		searchService.deleteRecord(recordIds.toString());
		return recordIds.size();
	}


	@Override
	public String filterByParams(String params, String pageNo, String pageSize, String order){
		return searchService.filter(params, pageNo, pageSize, order);
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
