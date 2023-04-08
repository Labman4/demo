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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.elpsykongroo.gateway.exception.ServiceException;
import com.elpsykongroo.gateway.utils.IPRegexUtils;
import com.elpsykongroo.services.elasticsearch.client.SearchService;
import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecord;
import jakarta.servlet.http.HttpServletRequest;

import com.elpsykongroo.gateway.common.CommonResponse;
import com.elpsykongroo.gateway.config.RequestConfig;
import com.elpsykongroo.gateway.config.RequestConfig.Record.Exclude;
import com.elpsykongroo.gateway.service.AccessRecordService;
import com.elpsykongroo.gateway.service.IPManagerService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

	public void saveAcessRecord(HttpServletRequest request) {
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
					searchService.saveRecord(record);
					log.info("request header------------{} ", result);
			    }
		  	}
		} catch (Exception e) {
			throw new ServiceException(e);
		}
	}

	@Override
	public String findAll(String pageNo, String pageSize, String order) {
		String records = null;
		try {
			records = searchService.findAllRecord(pageNo, pageSize ,order);
		} catch (NumberFormatException e) {
			throw new ServiceException(e);
		}
		return records;
	}

	@Override
	public CommonResponse<Integer> deleteRecord(String sourceIP, String ids) {
		List<String> recordIds = null;
		try {
			recordIds = new ArrayList<>();
			if (StringUtils.isNotEmpty(ids)) {
				recordIds = new ArrayList<String>(Arrays.asList(ids.split(",")));
			}

			if (StringUtils.isNotEmpty(sourceIP)) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(sourceIP);
				for (InetAddress addr: inetAddresses) {
					List<AccessRecord> accessRecord = searchService.findBySourceIP(addr.getHostAddress());
					for (AccessRecord record: accessRecord) {
						recordIds.add(record.getId());
					}
				}
			}
			searchService.deleteAllRecordById(recordIds);
		} catch (Exception e) {
			throw new ServiceException(e);
		}
		return CommonResponse.success(recordIds.size());
	}


	@Override
	public CommonResponse<List<AccessRecord>> filterByParams(String params, String pageNo, String pageSize) {
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNo), Integer.parseInt(pageSize));
		try {
			List<AccessRecord> records = new ArrayList<>();
			if (IPRegexUtils.vaildate(params)) {
				records = searchService.findBySourceIP(params);
			}
			if (IPRegexUtils.vaildateHost(params)) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(params);
				for (InetAddress addr: inetAddresses) {
					List<AccessRecord> accessRecord = searchService.findBySourceIP(addr.getHostAddress());
					records.addAll(accessRecord);
				}
			}
			records.addAll(searchService.findByUserAgentLike(params));
			records.addAll(searchService.findByAccessPathLike(params));
			records.addAll(searchService.findByRequestHeaderLike(params));
			int start = (int) pageable.getOffset();
			int end = (int) ((start + pageable.getPageSize()) > records.size() ? records.size() : (start + pageable.getPageSize()));
			Page<AccessRecord> page =
					new PageImpl<AccessRecord>(records.subList(start, end), pageable, records.size());
		    return CommonResponse.success(page.get().toList());
		} catch (Exception e) {
			throw new ServiceException(e);
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
