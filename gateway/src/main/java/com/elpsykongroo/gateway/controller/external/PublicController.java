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

package com.elpsykongroo.gateway.controller.external;

import com.elpsykongroo.base.common.CommonResponse;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.gateway.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import com.elpsykongroo.gateway.service.IPManagerService;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@CrossOrigin
@RestController
@RequestMapping("public")
public class PublicController {

	@Autowired
	private IPManagerService ipManagerService;

	@Autowired
	private RedisService redisService;

	@Autowired
	private MessageService messageService;


	@GetMapping("ip")
	public String accessIP(HttpServletRequest request) {
		return CommonResponse.string(ipManagerService.accessIP(request, ""));
	}

	@GetMapping("token/qrcode")
	public String qrToken(@RequestParam("text") String text, HttpServletResponse response) throws InterruptedException {
		String message = messageService.getMessage();
		int count = 0;
		while (StringUtils.isEmpty(message)) {
			message = messageService.getMessage();
			count++;
			Thread.sleep(5000);
			if (count>60) {
				return "";
			}
		}
		return message;
	}
}
