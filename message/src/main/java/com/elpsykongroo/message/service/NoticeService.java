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


package com.elpsykongroo.message.service;

import com.elpsykongroo.base.domain.search.repo.Notice;
import com.elpsykongroo.base.domain.search.repo.NoticeTopic;

import java.util.List;

public interface NoticeService {
    void register(String token, String timestamp, String user);

    void push(Notice notice);

    String findToken(String user);

    String noticeList(Notice notice);

    String noticeListByUser(String user, String draft);

    String sendNotice(Notice notice);

    List<NoticeTopic> topicList(List<String> topic);

    List<NoticeTopic> topicListByUser(String user);

    String addOrUpdateTopic(NoticeTopic noticeTopic);

    String deleteTopic(List<String> topics);

    void deleteNotice(List<String> ids);
}
