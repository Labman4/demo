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

package com.elpsykongroo.demo.common;

import com.elpsykongroo.demo.constant.Constant;
import lombok.Data;
import lombok.NoArgsConstructor;

 import org.springframework.stereotype.Component;

//@TypeHint(types = {
//		CommonResponse.class
//}, access = { TypeAccess.PUBLIC_CONSTRUCTORS, TypeAccess.PUBLIC_METHODS })
@Component
@Data
@NoArgsConstructor
public class CommonResponse<T> {

	private String code;

	private String msg;

	private T data;

	public CommonResponse(String code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public CommonResponse(String code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

	public CommonResponse<T> success(T data) {
		return new CommonResponse<T>(Constant.SUCCESS_CODE, Constant.SUCCESS_MSG, data);
	}

	public CommonResponse<T> error(String code, String msg) {
		return new CommonResponse<T>(code, msg);
	}
}
