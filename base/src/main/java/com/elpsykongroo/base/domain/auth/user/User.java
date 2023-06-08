/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.elpsykongroo.base.domain.auth.user;


import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;


@Data
@NoArgsConstructor
public class User{
    private String id;
    private Map<String, Object> userInfo;
    private String username;
    private String email;
    private String nickName;

    private String password;

    private boolean locked;

    private Instant createTime;

    private Instant updateTime;


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("{");
        sb.append("\"id\":\"").append(id).append("\",");
        sb.append("\"username\":\"").append(username).append("\",");
        sb.append("\"email\":\"").append(email).append("\",");
        sb.append("\"nickName\":\"").append(nickName).append("\",");
        sb.append("\"password\":\"").append(password).append("\",");
        sb.append("\"locked\":\"").append(locked).append("\",");
        sb.append("\"createTime\":\"").append(createTime).append("\",");
        sb.append("\"updateTime\":\"").append(updateTime).append("\"");
        sb.append('}');
        return sb.toString();
    }
}
