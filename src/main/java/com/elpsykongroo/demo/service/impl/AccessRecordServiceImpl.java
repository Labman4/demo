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

package com.elpsykongroo.demo.service.impl;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import com.elpsykongroo.demo.common.CommonResponse;
import com.elpsykongroo.demo.config.RequestConfig;
import com.elpsykongroo.demo.document.AccessRecord;
import com.elpsykongroo.demo.exception.ElasticException;
import com.elpsykongroo.demo.repo.AccessRecordRepo;
import com.elpsykongroo.demo.service.AccessRecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccessRecordServiceImpl implements AccessRecordService {
	private AccessRecordRepo accessRecordRepo;

	public AccessRecordServiceImpl(AccessRecordRepo accessRecordRepo) {
		this.accessRecordRepo = accessRecordRepo;
	}
//    @Autowired
//    private RedissonClient redissonClient;

	@Autowired
	private RequestConfig requestConfig;

	public void saveAcessRecord(HttpServletRequest request) {
		String recordExcludePath = requestConfig.getPath().getExclude().getRecord();
		if (!(StringUtils.isNotEmpty(recordExcludePath) && beginWithPath(recordExcludePath, request.getRequestURI()))) {
			Map<String, String> result = new HashMap<>();
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String key = (String) headerNames.nextElement();
				String value = request.getHeader(key);
				result.put(key, value);
			}
			AccessRecord record = new AccessRecord();
			record.setRequestHeader(result);
			record.setAccessPath(request.getRequestURI());
			record.setSourceIP(request.getRemoteAddr());
			record.setTimestamp(new Date());
			record.setUserAgent(request.getHeader("user-agent"));
			try {
				accessRecordRepo.save(record);
			}
			catch (RuntimeException e) {
				log.info("timeout");
			}
			log.info("request header------------{} ", result);
		}
	}

	@Override
	public CommonResponse<List<AccessRecord>> findAll(String pageNo, String pageSize, String order) {
		Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
		if ("1".equals(order)) {
			sort = Sort.by(Sort.Direction.ASC, "timestamp");
		}
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNo), Integer.parseInt(pageSize), sort);
		Page<AccessRecord> records = accessRecordRepo.findAll(pageable);
		return CommonResponse.success(records.get().toList());
	}

	@Override
	public CommonResponse<Integer> deleteRecord(String sourceIP, String ids) {
		List<String> recordIds = new ArrayList<>();
		if (StringUtils.isNotEmpty(ids)) {
			recordIds = Arrays.asList(ids.split(","));
		}
		try {
			if (StringUtils.isNotEmpty(sourceIP)) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(sourceIP);
				for (InetAddress addr: inetAddresses) {
					List<AccessRecord> accessRecord = accessRecordRepo.findBySourceIP(addr.getHostAddress());
					for (AccessRecord record: accessRecord) {
						recordIds.add(record.getId());
					}
				}
			}
			accessRecordRepo.deleteAllById(recordIds);
		}
		catch (Exception e) {
			throw new ElasticException(e);
		}
		return CommonResponse.success(recordIds.size());
	}


	@Override
	public CommonResponse<List<AccessRecord>> filterUserAgent(String path) {
		List<AccessRecord> accessRecords = accessRecordRepo.findByUserAgentLike(path);
		return CommonResponse.success(accessRecords);
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
