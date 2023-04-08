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
import com.elpsykongroo.services.elasticsearch.server.repo.IPRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/search/ip")
@Slf4j
public class IPManagerController {
	@Autowired
	private IPRepo ipRepo;

	@PutMapping("/add")
	public String saveIP(@RequestBody IPManage ipManage) {
		return ipRepo.save(ipManage).getAddress();
	}

	@GetMapping("/list")
	public String list(@RequestParam String pageNumber,
					   @RequestParam String pageSize) {
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize));
		return JsonUtils.toJson(ipRepo.findAll(pageable));
	}

	@GetMapping("/list/white")
	public String whiteList(@RequestParam String pageNumber,
							@RequestParam String pageSize) {
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize));
		return JsonUtils.toJson(ipRepo.findByIsBlackFalse(pageable));
	}

	@GetMapping("/list/black")
	public String blackList(@RequestParam String pageNumber,
							@RequestParam String pageSize) {
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize));
		return JsonUtils.toJson(ipRepo.findByIsBlackTrue(pageable));
	}

	@GetMapping("/white/list")
	public String whiteList() {
		return JsonUtils.toJson(ipRepo.findByIsBlackFalse());
	}

	@GetMapping("/black/list")
	public String blackList() {
		return JsonUtils.toJson(ipRepo.findByIsBlackTrue());
	}

	@GetMapping("/white/count")
	public String whiteCount(String address) {
		return JsonUtils.toJson(ipRepo.countByAddressAndIsBlackFalse(address));
	}

	@GetMapping("/black/count")
	public String blackCount(String address) {
		return JsonUtils.toJson(ipRepo.countByAddressAndIsBlackTrue(address));
	}

	@DeleteMapping("/black/delete")
	public String deleteBlack(String address) {
		try {
			ipRepo.deleteByAddressAndIsBlackTrue(address);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}

	@DeleteMapping("/white/delete")
	public String deleteWhite(String address) {
		try {
			ipRepo.deleteByAddressAndIsBlackFalse(address);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}

	@DeleteMapping("/delete")
	public String delete(String address) {
		try {
			ipRepo.deleteById(address);
			return "done";
		} catch (Exception e) {
			return "0";
		}
	}
}
