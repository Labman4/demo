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

import com.elpsykongroo.demo.common.CommonResponse;
import com.elpsykongroo.demo.domain.AccessRecord;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;


@Service
public interface AccessRecordService {

	CommonResponse<List<AccessRecord>> findAll(String pageNumber, String pageSize, String sort);

	CommonResponse<List<AccessRecord>> filterByParams(String params, String type);

	CommonResponse<Integer> deleteRecord(String sourceIP, String ids);

	void saveAcessRecord(HttpServletRequest request);

}
