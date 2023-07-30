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

package com.elpsykongroo.base.domain.storage.object;

import lombok.Data;
import lombok.NoArgsConstructor;

<<<<<<< HEAD:base/src/main/java/com/elpsykongroo/base/domain/storage/object/ListObjectResult.java
import java.time.Instant;

@Data
@NoArgsConstructor
public class ListObjectResult {
    private String key;
    private Instant timestamp;
    private Long size;

    public ListObjectResult(String key, Instant lastModified, Long size) {
        this.key = key;
        this.timestamp = lastModified;
        this.size = size;
    }
=======
@NoArgsConstructor
public class ServiceException extends RuntimeException {
	private static final long serialVersionUID = 5639223247225972389L;

	public ServiceException(Throwable cause) {
		super(cause);
	}

	public ServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceException(String message) {
		super(message);
	}
>>>>>>> main:base/src/main/java/com/elpsykongroo/base/domain/storage/object/ServiceException.java
}
