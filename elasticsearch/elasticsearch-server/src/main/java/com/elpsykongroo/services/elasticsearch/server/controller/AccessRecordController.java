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

import com.elpsykongroo.services.elasticsearch.client.dto.AccessRecordDto;
import com.elpsykongroo.services.elasticsearch.server.domain.AccessRecord;
import com.elpsykongroo.services.elasticsearch.server.service.AccessRecordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
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
	private AccessRecordService accessRecordService;

	@PutMapping("/add")
	public String saveAccessRecord(@RequestBody AccessRecord accessrecord) {
		try {
			log.debug("record add");
			accessRecordService.save(accessrecord);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}

	@GetMapping("/list")
	public ResponseEntity<List<AccessRecord>> findAll(@RequestParam String order,
												  @RequestParam String pageNumber,
												  @RequestParam String pageSize) {
		log.debug("record list");
		Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
		if ("1".equals(order)) {
			sort = Sort.by(Sort.Direction.ASC, "timestamp");
		}
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize), sort);
		return ResponseEntity.ok(accessRecordService.findAll(pageable).get().toList());
	}

	@GetMapping("/list/ip")
	public ResponseEntity<List<AccessRecord>> findByIP(@RequestParam String ip) {
		log.debug("record list ip");
		return ResponseEntity.ok(accessRecordService.findBySourceIP(ip));
	}

	@GetMapping("/list/path")
	public ResponseEntity<List<AccessRecord>> findByPath(@RequestParam String path) {
		log.debug("record path");
		return ResponseEntity.ok(accessRecordService.findByAccessPathLike(path));
	}

	@GetMapping("/list/agent")
	public ResponseEntity<List<AccessRecord>> findByUserAgent(@RequestParam String agent) {
		log.debug("record agent");
		return ResponseEntity.ok(accessRecordService.findByUserAgentLike(agent));
	}

	@GetMapping("/list/header")
	public ResponseEntity<List<AccessRecord>> findByHeader(@RequestParam String header) {
		log.debug("record header");
		return ResponseEntity.ok(accessRecordService.findByRequestHeaderLike(header));
	}

	@PostMapping("delete")
	public String deleteAllRecordById(@RequestBody List<String> ids) {
		try {
			log.debug("record delete");
			accessRecordService.deleteAllById(ids);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}

	@PostMapping("/filter")
	public ResponseEntity<List<AccessRecord>> filter(@RequestBody AccessRecordDto accessRecordDto) {
		try {
			log.debug("filter");
			return ResponseEntity.ok(accessRecordService.searchSimilar(accessRecordDto));
		} catch (Exception e) {
			log.error("filter error: {}", e.getMessage());
			return ResponseEntity.ofNullable(null);
		}
	}
}
