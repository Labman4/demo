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
import com.elpsykongroo.services.elasticsearch.server.domain.IPManage;
import com.elpsykongroo.services.elasticsearch.server.service.IPManageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@PutMapping
	public String save(@RequestBody IPManage ipManage) {
		log.debug("ip add");
		return CommonResponse.string(ipManageService.save(ipManage).getAddress());
	}

	@GetMapping("page")
	public String ipPageList(@RequestParam String pageNumber,
							@RequestParam String pageSize,
							@RequestParam("black") String black) {
		Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize));
		return CommonResponse.success(ipManageService.ipPageList(pageable, black));
	}

	@GetMapping
	public String ipList(@RequestParam("black") String black) {
		return CommonResponse.data(ipManageService.ipList(black));
	}

	@GetMapping("/count")
	public String whiteCount(@RequestParam("address") String address,
							 @RequestParam("black") String black) {
		return CommonResponse.string(ipManageService.count(address, black));
	}

	@DeleteMapping("/black/{address}")
	public String deleteBlack(@PathVariable String address) {
		try {
			log.debug("black delete");
			return CommonResponse.data(ipManageService.deleteByAddressAndIsBlackTrue(address));
		} catch (Exception e) {
			return CommonResponse.error(500, e.getMessage());
		}
	}

	@DeleteMapping("/white/{address}")
	public String deleteWhite(@PathVariable String address) {
		try {
			log.debug("white delete");
			return CommonResponse.data(ipManageService.deleteByAddressAndIsBlackFalse(address));
		} catch (Exception e) {
			return CommonResponse.error(500, e.getMessage());
		}
	}

	@DeleteMapping("/delete/{id}")
	public String delete(@PathVariable String id) {
		try {
			log.debug("delete by id");
			ipManageService.deleteById(id);
			return CommonResponse.success();
		} catch (Exception e) {
			return CommonResponse.error(500, e.getMessage());
		}
	}
}
