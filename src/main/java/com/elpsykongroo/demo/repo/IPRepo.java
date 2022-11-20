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

package com.elpsykongroo.demo.repo;


import java.util.List;

import com.elpsykongroo.demo.document.IPManage;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface IPRepo extends ElasticsearchRepository<IPManage, String> {

	List<IPManage> findByAddress(String address);

	List<IPManage> findByIsBlackTrue();

	List<IPManage> findByIsBlackFalse();

	long countByAddressAndIsBlackTrue(String address);

	long countByAddressAndIsBlackFalse(String address);

	List<IPManage> findByAddressAndIsBlackTrue(String address);

	List<IPManage> findByAddressAndIsBlackFalse(String address);

	void deleteByAddressAndIsBlackTrue(String address);

	void deleteByAddressAndIsBlackFalse(String address);

}
