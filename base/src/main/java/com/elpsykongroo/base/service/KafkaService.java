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

package com.elpsykongroo.base.service;

import com.elpsykongroo.base.domain.message.Send;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
public interface KafkaService {

    @RequestLine("POST /message")
    @Headers({
            "Content-Type: application/json"
    })
    void callback(Send send);

    @RequestLine("PUT /message")
    @Headers({
            "Content-Type: application/json"
    })
    String send(Send send);

    @RequestLine("DELETE /message/{ids}")
    void stop(@Param String ids);


    @RequestLine("DELETE /message/topic/{topic}")
    void deleteTopic(@Param String topic);

    @RequestLine("GET /message/{ids}")
    String listenerState(@Param String ids);


}