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

import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.services.elasticsearch.server.domain.IPManage;
import com.elpsykongroo.services.elasticsearch.server.service.IPManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("/search/ip")
@Slf4j
public class IPManagerController {
	@Autowired
	private IPManageService ipManageService;

	@PutMapping("/add")
	public String saveIP(@RequestBody IPManage ipManage) {
		log.debug("ip add");
		return ipManageService.save(ipManage).getAddress();
	}

	@GetMapping("/list")
	public String list(@RequestParam String pageNumber,
					   @RequestParam String pageSize) {
		log.debug("ip list");
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize));
		return JsonUtils.toJson(ipManageService.findAll(pageable));
	}

	@GetMapping("/list/white")
	public String whiteList(@RequestParam String pageNumber,
							@RequestParam String pageSize) {
		log.debug("ip white");
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize));
		return JsonUtils.toJson(ipManageService.findByIsBlackFalse(pageable));
	}

	@GetMapping("/list/black")
	public String blackList(@RequestParam String pageNumber,
							@RequestParam String pageSize) {
		log.debug("ip black");
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize));
		return JsonUtils.toJson(ipManageService.findByIsBlackTrue(pageable));
	}

	@GetMapping("/white/list")
	public String whiteList() {
		log.debug("white ip");
		return JsonUtils.toJson(ipManageService.findByIsBlackFalse());
	}

	@GetMapping("/black/list")
	public String blackList() {
		log.debug("black ip");
		return JsonUtils.toJson(ipManageService.findByIsBlackTrue());
	}

	@GetMapping("/white/count")
	public String whiteCount(String address) {
		log.debug("white count");
		return JsonUtils.toJson(ipManageService.countByAddressAndIsBlackFalse(address));
	}

	@GetMapping("/black/count")
	public String blackCount(String address) {
		log.debug("black count");
		return JsonUtils.toJson(ipManageService.countByAddressAndIsBlackTrue(address));
	}

	@DeleteMapping("/black/delete")
	public String deleteBlack(String address) {
		try {
			log.debug("black delete");
			ipManageService.deleteByAddressAndIsBlackTrue(address);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}

	@DeleteMapping("/white/delete")
	public String deleteWhite(String address) {
		try {
			log.debug("white delete");
			ipManageService.deleteByAddressAndIsBlackFalse(address);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}

	@DeleteMapping("/delete")
	public String delete(String id) {
		try {
			log.debug("delete by id");
			ipManageService.deleteById(id);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}
}
