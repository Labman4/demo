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

import java.util.List;

@Data
public class Notice {
    private String id;

    private String title;

    private String body;

    private String imageUrl;

    private List<String> topic;

    private boolean draft;

    private long timestamp;

    @Override
    public String toString() {
        return JsonUtils.toJson(this);
    }
}
