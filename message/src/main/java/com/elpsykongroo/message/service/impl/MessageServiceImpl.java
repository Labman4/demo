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


package com.elpsykongroo.message.service.impl;

import com.elpsykongroo.base.domain.message.Message;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.utils.PkceUtils;
import com.elpsykongroo.message.service.MessageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private RedisService redisService;

    @Override
    public String getMessageByPublicKey(String text) {
        String[] texts= text.split("\\*");
        String codeVerifier = texts[0];
        String timestamp = texts[1];
        String encodedVerifier = PkceUtils.verifyChallenge(codeVerifier);
        String challenge = redisService.get("PKCE-" + timestamp);
        if (StringUtils.isNotBlank(challenge) && challenge.equals(encodedVerifier)) {
            String message = redisService.get(text);
            if (StringUtils.isNotEmpty(message)) {
                redisService.set("PKCE-" + timestamp, "", "1");
            }
            return message;
        }
        return "";
    }

    @Override
    public void setMessage(Message message) {
        redisService.set(message.getKey(), message.getValue(), "");
    }
}
