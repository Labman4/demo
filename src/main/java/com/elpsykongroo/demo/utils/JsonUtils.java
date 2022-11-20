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

package com.elpsykongroo.demo.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

public final class JsonUtils {
	private JsonUtils() {
		throw new IllegalStateException("Utility class");
	}

	private static final Gson gson = new Gson();

	private static final ObjectMapper objectMapper = new ObjectMapper();
	public static String toJson(Object o) {
		try {
			return objectMapper.writeValueAsString(o);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

	}

	public static String toString(Object o) {
		return gson.toJson(o);
	}
}
