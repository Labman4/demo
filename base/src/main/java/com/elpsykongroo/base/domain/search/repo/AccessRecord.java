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

<<<<<<< HEAD:base/src/main/java/com/elpsykongroo/base/domain/search/repo/AccessRecord.java
package com.elpsykongroo.base.domain.search.repo;

import lombok.Data;

import java.util.Map;
=======
package com.elpsykongroo.demo.domain;

import java.util.Date;
import java.util.Map;

import lombok.Data;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
>>>>>>> main:gateway/src/main/java/com/elpsykongroo/gateway/domain/AccessRecord.java

@Data
public class AccessRecord {

	private String sourceIP;

	private String accessPath;

	private Map<String, String> requestHeader;

	private String userAgent;

	private String timestamp;

	private String id;

	@Override
	public String toString() {
		return id;
	}
}