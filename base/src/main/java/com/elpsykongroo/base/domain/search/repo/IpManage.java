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

<<<<<<< HEAD:base/src/main/java/com/elpsykongroo/base/domain/search/repo/IpManage.java
package com.elpsykongroo.base.domain.search.repo;
=======
package com.elpsykongroo.demo.domain;
>>>>>>> main:gateway/src/main/java/com/elpsykongroo/gateway/domain/IPManage.java

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

<<<<<<< HEAD:base/src/main/java/com/elpsykongroo/base/domain/search/repo/IpManage.java
=======
@Document(indexName = "ip")
>>>>>>> main:gateway/src/main/java/com/elpsykongroo/gateway/domain/IPManage.java
@Data
@NoArgsConstructor
public class IpManage {

	private String id;

	private String address;

	private String black;

	private String timestamp;

	public IpManage(String address) {
		this.address = address;
		this.black = "true";
		this.timestamp = Instant.now().toString();
	}

	public IpManage(String address, String black) {
		this.address = address;
		this.black = black;
		this.timestamp = Instant.now().toString();
	}
}
