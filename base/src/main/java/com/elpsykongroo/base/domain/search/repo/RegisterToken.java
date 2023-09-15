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

package com.elpsykongroo.base.domain.search.repo;

import com.elpsykongroo.base.utils.JsonUtils;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterToken {
    private String md5;
    private String token;
    private String timestamp;
    private String user;

    public RegisterToken(String token, String timestamp) {
        this.token = token;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
