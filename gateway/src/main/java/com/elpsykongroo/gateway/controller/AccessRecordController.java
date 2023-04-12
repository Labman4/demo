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
import com.elpsykongroo.gateway.service.AccessRecordService;
import lombok.extern.slf4j.Slf4j;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.UnknownHostException;

@CrossOrigin
@RestController
@RequestMapping("/record")
@Slf4j
public class AccessRecordController {
	@Autowired
	private AccessRecordService accessRecordService;

	@GetMapping("/access")
	public String getAccessRecord(@RequestParam("pageNumber") String pageNumber,
								  @RequestParam("pageSize") String pageSize,
								  @RequestParam("order") String order) {
		return accessRecordService.findAll(pageNumber, pageSize, order);
	}
	@DeleteMapping("/delete")
	public String deleteRecord(@RequestParam("sourceIP") String sourceIP, @RequestParam("id") String ids) {
		log.debug("delete accessRecord by sourceIP:{}", sourceIP);
		try {
			return CommonResponse.success(accessRecordService.deleteRecord(sourceIP, ids));
		} catch (UnknownHostException e) {
			return CommonResponse.error(HttpStatus.SC_CLIENT_ERROR, e.getMessage());
		}
	}

	@PostMapping("/filter")
	public String filter( @RequestParam("param") String params,
						  @RequestParam("pageNumber") String pageNumber,
						  @RequestParam("pageSize") String pageSize) {
		try {
			return CommonResponse.success(accessRecordService.filterByParams(params, pageNumber, pageSize));
		} catch (UnknownHostException e) {
			return CommonResponse.error(HttpStatus.SC_CLIENT_ERROR, e.getMessage());
		}
	}
}
