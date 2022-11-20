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

package com.elpsykongroo.demo.constant;

/**
 * @author labman4
 */

public interface Constant {
	String SUCCESS_CODE = "200";
	String SUCCESS_MSG = "success";
	String ERROR_CODE = "500";
	Integer  ACCESS_ERROR_CODE = 401;

	String ERROR_MSG = "error";
	Integer LIMIT_RESPONSE_CODE = 429;
	Integer EMPTY_RESPONSE_CODE = 404;

	Long REDIS_LOCK_WAIT_TIME = 5L;

	Long REDIS_LOCK_LEASE_TIME = 3L;
}
