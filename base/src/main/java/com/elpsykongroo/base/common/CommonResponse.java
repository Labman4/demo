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

import com.elpsykongroo.base.utils.JsonUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

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

	public CommonResponse(Integer code) {
		this.code = code;
	}

	public CommonResponse(Integer code, String msg) {
		this.code = code;
		this.msg = msg;
	}

	public CommonResponse(Integer code, T data) {
		this.code = code;
		this.data = data;
	}

	public CommonResponse(Integer code, String msg, T data) {
		this.code = code;
		this.msg  = msg;
		this.data = data;
	}

	public static <T> String data(T data) {
		return JsonUtils.toJson(data);
	}

	public static <T> String string(String data) {
		return data;
	}

	public static <T> String success() {
		return JsonUtils.toJson(new CommonResponse<T>(200));
	}

	public static <T> String success(T data) {
		return JsonUtils.toJson(new CommonResponse<T>(200, "success", data));
	}

	public static <T> String error(Integer code, String msg) {
		return JsonUtils.toJson(new CommonResponse<T>(code, msg));
	}


}
