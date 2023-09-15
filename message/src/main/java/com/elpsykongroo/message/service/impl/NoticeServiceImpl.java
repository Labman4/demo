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

import com.elpsykongroo.base.config.ServiceConfig;
import com.elpsykongroo.base.domain.search.repo.Notice;
import com.elpsykongroo.base.domain.search.repo.NoticeTopic;
import com.elpsykongroo.base.domain.search.repo.RegisterToken;
import com.elpsykongroo.base.domain.search.QueryParam;
import com.elpsykongroo.base.service.SearchService;
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.base.utils.MessageDigestUtils;
import com.elpsykongroo.message.service.NoticeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NoticeServiceImpl implements NoticeService {
    @Autowired
    private SearchService searchService;

    @Autowired
    private FirebaseApp firebaseApp;

    @Autowired
    private ServiceConfig seviceConfig;
    
    @Override
    public void register(String token, String timestamp, String user) {
        if(log.isDebugEnabled()) {
            log.debug("token receive:{}", token);
        }
        RegisterToken registerToken = new RegisterToken(token, timestamp);
        String md5 = MessageDigestUtils.md5(token.getBytes(StandardCharsets.UTF_8));
        registerToken.setMd5(md5);
        if (StringUtils.isNotEmpty(user)) {
            registerToken.setUser(user);
        }
        if (exist("md5", md5, "register_token", RegisterToken.class) > 0) {
            String script = "ctx._source.timestamp=params.timestamp;ctx._source.user=params.user;";
            List<String> params = new ArrayList<>();
            List<String> fields = new ArrayList<>();
            fields.add("md5");
            params.add(md5);
            Map<String, Object> update = new HashMap<>();
            update.put("timestamp", timestamp);
            update.put("user", user);
            if (StringUtils.isEmpty(user)) {
                String tokenResult = queryString(fields, params, "register_token", RegisterToken.class, "", "");
                List<RegisterToken> registerTokens = JsonUtils.toType(tokenResult, new TypeReference<List<RegisterToken>>() {});
                for (RegisterToken register : registerTokens) {
                    if (StringUtils.isNotEmpty(register.getUser())) {
                        script = "ctx._source.timestamp=params.timestamp";
                    }
                }
            }
            updateQuery(fields, params, update, "register_token", RegisterToken.class, script, 0);
        } else {
            save("register_token", registerToken);
        }
    }

    @Override
    public List<RegisterToken> findToken(List<String> users) {
        List<RegisterToken> tokens = new ArrayList<>();
        if (users == null || users.isEmpty()) {
            String result = matchAllQuery("register_token", RegisterToken.class);
            if (StringUtils.isNotEmpty(result)) {
                return JsonUtils.toType(result, new TypeReference<List<RegisterToken>>() {});
            }
        } else {
            List<String> fields = new ArrayList<>();
            List<String> params = new ArrayList<>();
            for (String user : users) {
                fields.add("user");
                params.add(user);
                String result = queryString(fields, params, "register_token", RegisterToken.class, "", "");
                if (StringUtils.isNotEmpty(result)) {
                    List<RegisterToken> registerTokens = JsonUtils.toType(result, new TypeReference<List<RegisterToken>>() {
                    });
                    tokens.addAll(registerTokens);
                }
            }
        }
        return tokens;
    }

    @Override
    public List<Notice> noticeList(Notice notice) {
        List<Notice> notices = new ArrayList<>();
        if (notice.getTopic() == null || notice.getTopic().isEmpty()) {
            String result = matchAllQuery("notification", Notice.class);
            if (StringUtils.isNotEmpty(result)) {
                return JsonUtils.toType(result, new TypeReference<List<Notice>>() {});
            }
        } else {
            for (String topic : notice.getTopic()) {
                List<String> fields = new ArrayList<>();
                fields.add("topic");
                fields.add("draft");
                List<String> params = new ArrayList<>();
                params.add(topic);
                params.add(String.valueOf(notice.isDraft()));
                String result = queryString(fields, params, "notification", Notice.class, "", "must");
                if (StringUtils.isNotEmpty(result)) {
                    List<Notice> noticeList = JsonUtils.toType(result, new TypeReference<List<Notice>>() {
                    });
                    notices.addAll(noticeList);
                }
            }
        }
        return notices;
    }

    @Override
    public List<Notice> noticeListByUser(String user, String draft) {
        List<NoticeTopic> noticeTopics = topicListByUser(user);
        List<String> topics = new ArrayList<>();
        for (NoticeTopic n : noticeTopics) {
            topics.add(n.getName());
        }
        if (topics.isEmpty()) {
            topics.add("");
        }
        Notice notice = new Notice();
        notice.setTopic(topics);
        notice.setDraft(Boolean.valueOf(draft));
        return noticeList(notice);
    }

    @Override
    public List<NoticeTopic> topicList(List<String> topics) {
        List<NoticeTopic> noticeTopics = new ArrayList<>();
        if (topics == null || topics.isEmpty()) {
            String result = matchAllQuery("notice_topic", NoticeTopic.class);
            if (StringUtils.isNotEmpty(result)) {
                return JsonUtils.toType(result, new TypeReference<List<NoticeTopic>>() {});
            }
        } else {
            List<String> fields = new ArrayList<>();
            List<String> params = new ArrayList<>();
            for (String topic : topics) {
                fields.add("name");
                params.add(topic);
                String result = queryString(fields, params, "notice_topic", NoticeTopic.class, "", "");
                if (StringUtils.isNotEmpty(result)) {
                    List<NoticeTopic> notices = JsonUtils.toType(result, new TypeReference<List<NoticeTopic>>() {
                    });
                    noticeTopics.addAll(notices);
                }
            }
        }
        return noticeTopics;
    }

    @Override
    public List<NoticeTopic> topicListByUser(String user) {
        List<NoticeTopic> noticeTopics = new ArrayList<>();
        String publicResult = existQuery("users", "notice_topic", NoticeTopic.class, "must_not");
        if (StringUtils.isNotEmpty(publicResult)) {
            List<NoticeTopic> notices = JsonUtils.toType(publicResult, new TypeReference<List<NoticeTopic>>() {});
            if (StringUtils.isEmpty(user)) {
                return notices;
            } else {
                noticeTopics.addAll((notices));
            }
        }
        List<String> fields = new ArrayList<>();
        List<String> params = new ArrayList<>();
        fields.add("users");
        params.add(user);
        String result = queryString(fields, params, "notice_topic", NoticeTopic.class, "", "");
        if (StringUtils.isNotEmpty(result)) {
            List<NoticeTopic> notices = JsonUtils.toType(result, new TypeReference<List<NoticeTopic>>() {
            });
            noticeTopics.addAll(notices);
        }
        return noticeTopics;
    }

    @Override
    public String deleteTopic(List<String> topics) {
       List<String> fields = new ArrayList<>();
       for (int i= 0; i < topics.size(); i++) {
           fields.add("name");
       }
       return queryString(fields, topics, "notice_topic", NoticeTopic.class, "deleteQuery", "should");
    }

    @Override
    public void deleteNotice(List<String> ids) {
        List<String> fields = new ArrayList<>();
        for (int i= 0; i < ids.size(); i++) {
            fields.add("id");
        }
        QueryParam queryParam = new QueryParam();
        queryParam.setIds(ids);
        queryParam.setOperation("delete");
        queryParam.setIndex("notification");
        searchService.query(queryParam);
    }

    @Override
    public String addOrUpdateTopic(NoticeTopic noticeTopic) {
        if (StringUtils.isEmpty(noticeTopic.getName())) {
            return "";
        }
        if (exist("name", noticeTopic.getName(), "notice_topic", NoticeTopic.class) == 0) {
            return save("notice_topic", noticeTopic);
        } else {
            String script = "ctx._source.users=params.users;ctx._source.registers=params.registers;";
            List<String> fields = new ArrayList<>();
            List<String> params = new ArrayList<>();
            fields.add("name");
            params.add(noticeTopic.getName());
            int updated = 0;
            Map<String, Object> update = new HashMap<>();
            update.put("users", noticeTopic.getUsers());
            update.put("registers", noticeTopic.getRegisters());
            updated = updateQuery(fields, params, update,
                    "notice_topic", NoticeTopic.class, script, updated);
            return String.valueOf(updated);
        }
    }

    private int exist(String field, String param, String index, Class type) {
        List<String> fields = new ArrayList<>();
        fields.add(field);
        List<String> params = new ArrayList<>();
        params.add(param);
        String count = queryString(fields, params, index, type, "count", "");
        return StringUtils.isNotBlank(count) ? Integer.parseInt(count) : 0;
    }

    private String save(String index, Object entity) {
        try {
            QueryParam queryParam = new QueryParam();
            queryParam.setIndex(index);
            queryParam.setOperation("save");
            queryParam.setEntity(entity);
            return searchService.query(queryParam);
        } catch (Exception e) {
            return "";
        }
    }

    private String existQuery(String field, String index, Class type, String boolType) {
        try {
            QueryParam queryParam = new QueryParam();
            queryParam.setIndex(index);
            queryParam.setType(type);
            queryParam.setField(field);
            queryParam.setOperation("exist");
            queryParam.setBoolType(boolType);
            queryParam.setBoolQuery(true);
            return searchService.query(queryParam);
        } catch (Exception e) {
            return "";
        }
    }

    private String queryString(List<String> fields, List<String> params, String index, Class type, String operation, String boolType) {
        try {
            QueryParam queryParam = new QueryParam();
            queryParam.setIndex(index);
            queryParam.setType(type);
            queryParam.setBoolQuery(true);
            queryParam.setFields(fields);
            queryParam.setQueryStringParam(params);
            if (StringUtils.isNotEmpty(boolType)) {
                queryParam.setBoolType(boolType);
            }
            if (StringUtils.isNotEmpty(operation)) {
                queryParam.setOperation(operation);
            }
            return searchService.query(queryParam);
        } catch (Exception e) {
            return "";
        }
    }

    private int updateQuery(List<String> fields, List<String> params, Map<String, Object> update, String index, Class type, String script, int updated) {
        try {
            QueryParam queryParam = new QueryParam();
            queryParam.setUpdateParam(update);
            queryParam.setScript(script);
            queryParam.setBoolType("should");
            queryParam.setBoolQuery(true);
            queryParam.setOperation("updateQuery");
            queryParam.setType(type);
            queryParam.setIndex(index);
            queryParam.setFields(fields);
            queryParam.setQueryStringParam(params);
            String result = searchService.query(queryParam);
            updated += Integer.parseInt(result);
            return updated;
        } catch (Exception e) {
            return 0;
        }
    }

    private String matchAllQuery(String index, Class type) {
        try {
            QueryParam queryParam = new QueryParam();
            queryParam.setIndex(index);
            queryParam.setType(type);
            return searchService.query(queryParam);
        } catch (Exception e) {
            return "";
        }
    }

    private int termQuery(String field, String param, List<String> updateParam, List<Object> updateParams, String index, Class type, String script, int updated) {
        try {
            QueryParam queryParam = new QueryParam();
            Map<String, Object> update = new HashMap<>();
            for (int i = 0; i< updateParam.size(); i++) {
                update.put(updateParam.get(i), updateParams.get(i));
            }
            queryParam.setUpdateParam(update);
            queryParam.setScript(script);
            queryParam.setOperation("updateQuery");
            queryParam.setType(type);
            queryParam.setIndex(index);
            queryParam.setField(field);
            queryParam.setParam(param);
            String result = searchService.query(queryParam);
            updated += Integer.parseInt(result);
            return updated;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public String sendNotice(Notice notice) {
        notice.setTimestamp(Instant.now().toEpochMilli());
        String result = save("notification", notice);
        push(notice);
        return result;
    }

    @Override
    public void push(Notice notice) {
        if (!notice.isDraft()) {
            List<String> registrationTokens = new ArrayList<>();
            Notification notification = Notification.builder()
                    .setBody(notice.getBody())
                    .setTitle(notice.getTitle())
                    .setImage(notice.getImageUrl()).build();
            List<NoticeTopic> noticeTopics = topicList(notice.getTopic());
            for (NoticeTopic n : noticeTopics) {
                if (n.getRegisters() != null && !n.getRegisters().isEmpty()) {
                    registrationTokens.addAll(n.getRegisters());
                }
                if (n.getUsers() == null || n.getUsers().isEmpty()) {
                    findTokenByUser(registrationTokens, new ArrayList<>());
                } else {
                    findTokenByUser(registrationTokens,  n.getUsers());
                }
            }
            pushNotice(registrationTokens, notification);
            List<String> params = new ArrayList<>();
            params.add("draft");
            params.add("timestamp");
            List<Object> values = new ArrayList<>();
            values.add(false);
            values.add(Instant.now().toEpochMilli());
            String script = "ctx._source.draft=params.draft;ctx._source.timestamp=params.timestamp;";
            termQuery("_id", notice.getId(), params, values,
                    "notification", Notice.class, script, 0);
        }
    }

    private void pushNotice(List<String> token, Notification notification) {
        if (token.isEmpty()) {
            return;
        }
        List<String> tokens = checkToken(token);
        if (log.isDebugEnabled()) {
            log.debug("validToken:{}", tokens.size());
        }
        WebpushFcmOptions webpushFcmOptions = WebpushFcmOptions.builder().setLink(seviceConfig.getUrl().getLoginPage()).build();
        WebpushConfig webpushConfig  = WebpushConfig.builder().setFcmOptions(webpushFcmOptions).build();
        if (!tokens.isEmpty()) {
            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(notification)
                    .setWebpushConfig(webpushConfig)
                    .build();
            BatchResponse response = null;
            try {
                response = FirebaseMessaging.getInstance(firebaseApp).sendMulticast(message);
                if (log.isDebugEnabled()) {
                    log.debug("push result success:{}, failed: {}",  response.getSuccessCount(), response.getFailureCount());
                }
            } catch (FirebaseMessagingException e) {
                throw new RuntimeException(e);
            }
        }
//            httpPost.addHeader("Authorization", "Bearer " + credentials.getAccessToken().getTokenValue());
//            URI uri = new URI("https://fcm.googleapis.com/v1/projects/elpsykongroo/messages:send");
//            Security.addProvider(new BouncyCastleProvider());
//            Notification notification = new Notification(subscription, "test");
//            PushService pushService = new PushService("apikey");
//            HttpPost httpPost = pushService.preparePost(notification, Encoding.AES128GCM);
//            httpPost.setURI(uri);
//            CloseableHttpAsyncClient closeableHttpAsyncClient = HttpAsyncClients.custom()
//                    .setProxy(proxy)
//                    .setRoutePlanner(routePlanner)
//                    .build();
//            closeableHttpAsyncClient.start();
//            HttpResponse httpResponse = (HttpResponse) closeableHttpAsyncClient.execute(httpPost, new ClosableCallback(closeableHttpAsyncClient)).get();
//            System.out.println(httpResponse);
    }

    private List<String> checkToken(List<String> tokens) {
        if (log.isDebugEnabled()) {
            log.debug("checkToken:{}", tokens.size());
        }
        List<String> validToken = new ArrayList<>();
        for (String token : tokens) {
            if (StringUtils.isNotBlank(token)) {
                Message message = Message.builder()
                        .setToken(token)
                        .build();
                try {
                    FirebaseMessaging.getInstance(firebaseApp).send(message, true);
                    validToken.add(token);
                } catch (FirebaseMessagingException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("token: {} is invalid", token);
                    }
                    removeInvalidToken(token);
                }
            }
        }
        return validToken.stream().distinct().collect(Collectors.toList());
    }

    private void removeInvalidToken(String token) {
        if(log.isDebugEnabled()) {
            log.debug("token invalid:{}", token);
        }
        List<String> params = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        params.add(token);
        fields.add("token");
        queryString(fields, params, "register_token", RegisterToken.class, "deleteQuery", "should");
    }

    private void findTokenByUser(List<String> registrationTokens, List<String> user) {
        List<RegisterToken> token = findToken(user);
        for (RegisterToken register : token) {
            registrationTokens.add(register.getToken());
        }
    }
}
