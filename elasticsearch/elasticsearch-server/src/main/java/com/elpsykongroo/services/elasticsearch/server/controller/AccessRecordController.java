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
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.services.elasticsearch.server.domain.AccessRecord;
import com.elpsykongroo.services.elasticsearch.server.repo.AccessRecordRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/search/record")
@Slf4j
public class AccessRecordController {
	@Autowired
	private AccessRecordRepo accessRecordRepo;

	@PutMapping("/add")
	public String saveAccessRecord(@RequestBody AccessRecord accessRecord) {
		try {
			accessRecordRepo.save(accessRecord);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}

	@GetMapping("/list")
	public String findAll(@RequestParam String order,
						  @RequestParam String pageNumber,
						  @RequestParam String pageSize) {
		Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
		if ("1".equals(order)) {
			sort = Sort.by(Sort.Direction.ASC, "timestamp");
		}
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize), sort);
		return JsonUtils.toJson(CommonResponse.success(accessRecordRepo.findAll(pageable).get().toList()));
	}

	@GetMapping("/list/ip")
	public String findByIP(@RequestParam String ip) {
		return JsonUtils.toJson(accessRecordRepo.findBySourceIP(ip));
	}

	@GetMapping("/list/path")
	public String findByPath(@RequestParam String path) {
		return JsonUtils.toJson(accessRecordRepo.findByAccessPathLike(path));
	}

	@GetMapping("/list/agent")
	public String findByUserAgent(@RequestParam String agent) {
		return JsonUtils.toJson(accessRecordRepo.findByUserAgentLike(agent));
	}

	@GetMapping("/list/header")
	public String findByHeader(@RequestParam String header) {
		return JsonUtils.toJson(accessRecordRepo.findByRequestHeaderLike(header));
	}

	@PostMapping("delete")
	public String deleteAllRecordById(@RequestBody List<String> ids) {
		try {
			accessRecordRepo.deleteAllById(ids);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}
}
