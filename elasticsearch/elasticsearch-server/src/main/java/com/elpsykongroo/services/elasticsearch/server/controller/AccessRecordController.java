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

package com.elpsykongroo.services.elasticsearch.server.controller;

import com.elpsykongroo.base.common.CommonResponse;
import com.elpsykongroo.services.elasticsearch.server.domain.AccessRecord;
import com.elpsykongroo.services.elasticsearch.server.service.AccessRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/search/record")
@Slf4j
public class AccessRecordController {
	@Autowired
	private AccessRecordService accessRecordService;

	@PutMapping
	public String save(@RequestBody AccessRecord accessrecord) {
		try {
			log.debug("record add");
			return CommonResponse.success(accessRecordService.save(accessrecord));
		} catch (Exception e) {
			return CommonResponse.error(500, e.getMessage());
		}
	}

	@GetMapping
	public String pageList(@RequestParam String order,
						   @RequestParam String pageNumber,
						   @RequestParam String pageSize) {
		log.debug("record list");
		return CommonResponse.success(accessRecordService.findAll(pageNumber, pageSize, order));
	}

	@GetMapping("ip")
	public String ipList(@RequestParam("ip") String ip) {
		log.debug("record ip list");
		return CommonResponse.data(accessRecordService.findBySourceIP(ip));
	}

	@DeleteMapping("{ids}")
	public String deleteAllRecordById(@PathVariable String ids) {
		try {
			log.debug("record delete");
			accessRecordService.deleteAllById(Arrays.stream(ids.split(",")).collect(Collectors.toList()));
			return CommonResponse.success();
		} catch (Exception e) {
			return CommonResponse.error(500, e.getMessage());
		}
	}

	@PostMapping
	public String filter(@RequestParam("params") String params,
						 @RequestParam("pageNumber") String pageNumber,
						 @RequestParam("pageSize") String pageSize,
						 @RequestParam("order") String order) {
		try {
			log.debug("filter");
			return CommonResponse.data(accessRecordService.filter(params, pageNumber, pageSize, order));
		} catch (Exception e) {
			log.error("filter error: {}", e.getMessage());
			return CommonResponse.error(500, e.getMessage());
		}
	}
}
