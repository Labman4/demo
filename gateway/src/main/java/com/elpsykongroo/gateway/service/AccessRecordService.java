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

package com.elpsykongroo.gateway.service;

import java.net.UnknownHostException;
import java.util.List;

import com.elpsykongroo.base.domain.search.repo.AccessRecord;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;


@Service
public interface AccessRecordService {

	String findAll(String pageNumber, String pageSize, String sort);

	String filterByParams(String params, String pageNumber, String pageSize, String order);

	String deleteRecord(List<String> param) throws UnknownHostException;

	void saveAccessRecord(HttpServletRequest request);

	void saveRecord(AccessRecord record);

}
