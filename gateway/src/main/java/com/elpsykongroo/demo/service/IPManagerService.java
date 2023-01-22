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

package com.elpsykongroo.demo.service;

import java.util.List;

import com.elpsykongroo.demo.exception.ServiceException;
import jakarta.servlet.http.HttpServletRequest;

import com.elpsykongroo.demo.common.CommonResponse;
import com.elpsykongroo.demo.domain.IPManage;

import org.springframework.stereotype.Service;


@Service
public interface IPManagerService {

	CommonResponse<List<String>> add(String address, String isBlack) throws ServiceException;

	CommonResponse<List<IPManage>> list(String isBlack, String pageNumber, String pageSize);

	CommonResponse<String> patch(String addresses, String isBlack, String ids) throws ServiceException;

	String accessIP(HttpServletRequest request, String type);

	Boolean blackOrWhiteList(HttpServletRequest request, String isBlack) throws ServiceException;

	boolean filterByIpOrList(HttpServletRequest request, String ip, String accessIP);

	boolean filterByIp(HttpServletRequest request, String ip, String accessIP);

}
