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

package com.elpsykongroo.gateway.controller;

import com.elpsykongroo.base.common.CommonResponse;
import com.elpsykongroo.base.domain.search.repo.AccessRecord;
import com.elpsykongroo.gateway.service.AccessRecordService;
import lombok.extern.slf4j.Slf4j;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("record")
@Slf4j
public class AccessRecordController {
	@Autowired
	private AccessRecordService accessRecordService;

	@GetMapping
	public String recordPageList(@RequestParam String pageNumber,
								 @RequestParam String pageSize,
								 @RequestParam String order,
								 @RequestParam(required = false) String id) {
		return CommonResponse.string(accessRecordService.findAll(pageNumber, pageSize, order, id));
	}

	@DeleteMapping("{param}")
	public String deleteRecord(@PathVariable List<String> param) {
		if (log.isDebugEnabled()) {
			log.debug("deleteRecord:{}", param);
		}
		try {
			return accessRecordService.deleteRecord(param);
		} catch (UnknownHostException e) {
			return "0";
		}
	}

	@PostMapping
	public String filter(@RequestParam String params,
						 @RequestParam(required = false) String pageNumber,
						 @RequestParam(required = false) String pageSize,
						 @RequestParam(required = false) String order,
						 @RequestParam(required = false) String id) {
		try {
			return CommonResponse.string(accessRecordService.filterByParams(params, pageNumber, pageSize, order, id));
		} catch (Exception e) {
			return CommonResponse.error(HttpStatus.SC_CLIENT_ERROR, e.getMessage());
		}
	}

	@PutMapping
	public void saveRecord(@RequestBody AccessRecord record) {
		if (log.isDebugEnabled()) {
			log.debug("saveRecord:{}", record.getSourceIP());
		}
		accessRecordService.saveRecord(record);
	}
}
