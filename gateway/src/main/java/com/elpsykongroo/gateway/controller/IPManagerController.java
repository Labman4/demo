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

	@PutMapping
	public String add(@RequestParam String address, @RequestParam String black) {
		log.debug("add sourceIP:{}, black:{}", address, black);
			try {
				return CommonResponse.success(ipManagerService.add(address, black));
			}
			catch (Exception e) {
				return CommonResponse.error
						(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}
	@GetMapping
	public String ipPageList(@RequestParam String black, @RequestParam String pageNumber, @RequestParam String pageSize) {
		log.debug("black:{}, pageNumber:{}, pageSize:{}", black, pageNumber, pageSize);
		return CommonResponse.string(ipManagerService.list(black, pageNumber, pageSize));
	}

	@PatchMapping
	public String delete(@RequestParam("address") String addresses, @RequestParam("black") String isBlack, @RequestParam("id") String ids) {
		try {
			log.debug("black:{}, addresses:{}, ids:{}", isBlack, addresses, ids);
			ipManagerService.patch(addresses, isBlack, ids);
			return CommonResponse.success();
		} catch (Exception e) {
			return CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}
}
