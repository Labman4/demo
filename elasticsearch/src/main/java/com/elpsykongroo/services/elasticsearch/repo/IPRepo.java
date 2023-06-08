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

package com.elpsykongroo.services.elasticsearch.repo;


import com.elpsykongroo.services.elasticsearch.domain.IPManage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPRepo extends ElasticsearchRepository<IPManage, String> {

	String findByIsBlackTrue();

	String findByIsBlackFalse();

	List<IPManage> findByIsBlackTrue(Pageable pageable);

	List<IPManage> findByIsBlackFalse(Pageable pageable);

	String countByAddressAndIsBlackTrue(String address);

	String countByAddressAndIsBlackFalse(String address);

	String deleteByAddressAndIsBlackTrue(String address);

	String deleteByAddressAndIsBlackFalse(String address);

}