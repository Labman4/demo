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

package com.elpsykongroo.message.controller;

import com.elpsykongroo.base.domain.search.repo.Notice;
import com.elpsykongroo.base.domain.search.repo.NoticeTopic;
import com.elpsykongroo.message.service.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(originPatterns = "*", allowCredentials = "true")
@Slf4j
@RestController
@RequestMapping("notice")
public class NoticeController {
    @Autowired
    private NoticeService noticeService;

    @PostMapping
    public String noticeList(@RequestBody Notice notice) {
        return noticeService.noticeList(notice).toString();
    }

    @GetMapping("user")
    public String noticeListByUser(@RequestParam String user, @RequestParam String draft) {
        return noticeService.noticeListByUser(user, draft).toString();
    }

    @DeleteMapping
    public void deleteNotice(@RequestParam List<String> ids) {
        noticeService.deleteNotice(ids);
    }

    @GetMapping("token")
    public String getToken(@RequestParam(required = false) List<String> user) {
        return noticeService.findToken(user).toString();
    }

    @PutMapping("register")
    public void registerToken(@RequestParam String token,
                              @RequestParam String timestamp,
                              @RequestParam(required = false) String user) {
        noticeService.register(token, timestamp, user);
    }

    @PostMapping("push")
    public void subscribe(@RequestBody Notice notice)  {
        noticeService.push(notice);
    }

    @PutMapping
    public void send(@RequestBody Notice notice)  {
        noticeService.sendNotice(notice);
    }

    @PutMapping("/topic")
    public String addTopic(@RequestBody NoticeTopic noticeTopic)  {
        return noticeService.addOrUpdateTopic(noticeTopic);
    }

    @DeleteMapping("/topic")
    public void deleteTopic(@RequestParam List<String> topic)  {
        noticeService.deleteTopic(topic);
    }

    @GetMapping("/topic")
    public String getTopic(@RequestParam List<String> topic)  {
        return noticeService.topicList(topic).toString();
    }

}
