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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;
import java.util.List;


@CrossOrigin
@RestController
@RequestMapping("ip")
@Slf4j
public class IPManagerController {
	@Autowired
	private IPManagerService ipManagerService;

	@PutMapping
	public String add(@RequestParam List<String> address, @RequestParam String black) {
		if (log.isDebugEnabled()) {
			log.debug("add sourceIP:{}, black:{}", address, black);
		}
		try {
			return CommonResponse.data(ipManagerService.add(address, black));
		} catch (UnknownHostException e) {
			if (log.isErrorEnabled()) {
				log.error("error host: {}", e.getMessage());
			}
			return "0";
		}
	}
	@GetMapping
	public String ipPageList(@RequestParam String black,
							 @RequestParam String pageNumber,
							 @RequestParam String pageSize,
							 @RequestParam String order,
							 @RequestParam String id) {
		if (log.isDebugEnabled()) {
			log.debug("ipPageList black:{}, pageNumber:{}, pageSize:{}", black, pageNumber, pageSize);
		}
		return CommonResponse.string(ipManagerService.list(black, pageNumber, pageSize, order, id));
	}

	@PatchMapping
	public String patch(@RequestParam List<String> address, @RequestParam String black, @RequestParam("id") String ids) {
		try {
			if (log.isDebugEnabled()) {
				log.debug("ip patch black:{}, address:{}, ids:{}", black, address, ids);
			}
			return ipManagerService.patch(address, black, ids);
		} catch (Exception e) {
			return CommonResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}

	@PostMapping
	public String blackOrWhiteList(@RequestParam String black, @RequestParam String ip) {
		if (log.isDebugEnabled()) {
			log.debug("blackOrWhite black:{}, ip:{}", black, ip);
		}
		return CommonResponse.data(ipManagerService.blackOrWhiteList(null, black, ip));
	}
}
