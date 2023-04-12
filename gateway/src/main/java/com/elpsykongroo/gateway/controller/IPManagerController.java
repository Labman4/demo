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
import com.elpsykongroo.gateway.service.IPManagerService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@CrossOrigin
@RestController
@RequestMapping("/ip")
@Slf4j
public class IPManagerController {
	@Autowired
	private IPManagerService ipManagerService;

	@PutMapping("/manage/add")
	public String addBlacklist(@RequestParam("address") String address, @RequestParam("black") String isBlack) {
		log.debug("add sourceIP:{}, black:{}", address, isBlack);
			try {
				return CommonResponse.success(ipManagerService.add(address, isBlack));
			}
			catch (Exception e) {
				return CommonResponse.error
						(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}
	@GetMapping("/manage/list")
	public String blacklist(@RequestParam("black") String isBlack, @RequestParam("pageNumber") String pageNumber, @RequestParam("pageSize") String pageSize) {
		log.debug("black:{}, pageNumber:{}, pageSize:{}", isBlack, pageNumber, pageSize);
		return ipManagerService.list(isBlack, pageNumber, pageSize);
	}

	@PatchMapping("/manage/patch")
	public String updateBlacklist(@RequestParam("address") String addresses, @RequestParam("black") String isBlack, @RequestParam("id") String ids) {
		try {
			log.debug("black:{}, addresses:{}, ids:{}", isBlack, addresses, ids);
			ipManagerService.patch(addresses, isBlack, ids);
			return CommonResponse.success();
		} catch (Exception e) {
			return CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}
}
