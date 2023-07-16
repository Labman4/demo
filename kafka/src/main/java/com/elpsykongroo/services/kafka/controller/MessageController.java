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

package com.elpsykongroo.services.kafka.controller;

import com.elpsykongroo.base.domain.message.Send;
import com.elpsykongroo.services.kafka.service.KafkaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("message")
@Slf4j
public class MessageController {

    @Autowired
    private KafkaService kafkaService;

    @PutMapping
    public String send(@RequestBody Send send) {
        if (log.isDebugEnabled()) {
            log.debug("send topic:{}, key:{}, message:{}, data:{}",
                    send.getTopic(), send.getKey(), send.getMessage(), send.getData().length);
        }
        return kafkaService.send(send);
    }
    @PostMapping
    public void listen(@RequestBody Send send) {
        if (log.isDebugEnabled()) {
            log.debug("callback id:{}, groupId:{}, topic:{}, callback:{}",
                    send.getId(), send.getGroupId(), send.getTopic(), send.getCallback());
        }
        kafkaService.callback(send.getId(), send.getGroupId(), send.getTopic(), send.getCallback(), send.isManualStop());
    }

    @DeleteMapping("{ids}")
    public void stop( @PathVariable String ids) {
        if (log.isDebugEnabled()) {
            log.debug("stopListener ids:{}", ids);
        }
        kafkaService.stopListen(ids);
    }

    @DeleteMapping("topic/{topic}")
    public void deleteTopic( @PathVariable String topic) {
        if (log.isDebugEnabled()) {
            log.debug("deleteTopic:{}", topic);
        }
        kafkaService.deleteTopic(topic);
    }

    @GetMapping("{ids}")
    public String listenersState(@PathVariable String ids) {
        if (log.isDebugEnabled()) {
            log.debug("listListener ids:{}", ids);
        }
        return kafkaService.listenersState(ids);
    }
}
