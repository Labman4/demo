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

package com.elpsykongroo.base.handler;


import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.UnknownHostException;

@Slf4j
@RestControllerAdvice(basePackages = "com.elpsykongroo")
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public void handleFeignException(FeignException ex) {
        if (log.isDebugEnabled()) {
            log.debug("feign error");
        }
    }

    @ExceptionHandler(UnknownHostException.class)
    public void handleHostException(UnknownHostException ex) {
        if (log.isDebugEnabled()) {
            log.debug("unknown host");
        }
    }
}
