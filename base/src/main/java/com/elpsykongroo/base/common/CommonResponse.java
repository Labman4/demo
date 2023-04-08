/*
 * Copyright 2019-2021 the original author or authors.
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

package com.elpsykongroo.base.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.hc.core5.http.HttpStatus;

import java.io.Serial;
import java.io.Serializable;

//@TypeHint(types = {
//		CommonResponse.class
//}, access = { TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.PUBLIC_METHODS })
@Data
@NoArgsConstructor
public class CommonResponse<T> implements Serializable{

	@Serial
	private static final long serialVersionUID = 4018364510993114709L;
	
	private Integer code;

	private String msg;

	private T data;

	public CommonResponse(Integer code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public CommonResponse(Integer code, T data) {
		this.code = code;
		this.data = data;
	}

	public static <T> CommonResponse<T> success(T data) {
		return new CommonResponse<T>(HttpStatus.SC_OK, data);
	}

	public static <T> CommonResponse<T> error(Integer code, String msg) {
		return new CommonResponse<T>(code, msg);
	}
}
