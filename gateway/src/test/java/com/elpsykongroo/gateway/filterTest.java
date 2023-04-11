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

package com.elpsykongroo.gateway;

import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.gateway.config.RequestConfig;
import com.elpsykongroo.gateway.filter.ThrottlingFilter;
import com.elpsykongroo.gateway.service.AccessRecordService;
import com.elpsykongroo.gateway.service.IPManagerService;
import com.elpsykongroo.gateway.service.impl.AccessRecordServiceImpl;
import com.elpsykongroo.gateway.service.impl.IPMangerServiceImpl;
import com.elpsykongroo.services.elasticsearch.client.SearchService;
import com.elpsykongroo.services.elasticsearch.client.dto.IPManage;
import com.elpsykongroo.services.elasticsearch.client.impl.SearchServiceImpl;
import com.elpsykongroo.services.redis.client.RedisService;
import com.elpsykongroo.services.redis.client.impl.RedisServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Parameter;
import org.springframework.boot.autoconfigure.web.client.RestTemplateBuilderConfigurer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.mockserver.model.MediaType;

import java.io.IOException;
import java.util.Collections;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


public class filterTest {
    private MockServletContext servletContext;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    private MockServerClient mockServerClient;

    @Before
    public void setUp() {
        servletContext = new MockServletContext();
        request = new MockHttpServletRequest(servletContext);
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        mockServerClient = startClientAndServer(8880);
        IPManage ipManage = new IPManage();
        // place in beforeEach not work
        mockServerClient
                .when(request()
                        .withPath("/redis/get")
                        .withMethod("GET")
                        .withQueryStringParameters(
                                Parameter.param("key", "blackList")
                        ))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(""));
        mockServerClient
                .when(request()
                        .withPath("/redis/set")
                        .withMethod("POST"))
                .respond(response()
                        .withStatusCode(200));
        mockServerClient.when(request().withPath("/search/ip/list.*"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(JsonUtils.toJson(Collections.singleton(ipManage)), MediaType.APPLICATION_JSON));
        mockServerClient.when(request().withPath("/search/ip/count.*"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody("0", MediaType.APPLICATION_FORM_URLENCODED));
        mockServerClient.when(request().withPath("/search/ip/black/list"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(JsonUtils.toJson(Collections.singleton(ipManage)), MediaType.APPLICATION_JSON));
        mockServerClient.when(request().withPath("/search/ip/white/list"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(JsonUtils.toJson(Collections.singleton(ipManage)), MediaType.APPLICATION_JSON));
    }

    @After
    public void tearDown() {
        mockServerClient.stop();
    }

    @Test
    public void filter() throws ServletException, IOException {
        RequestConfig requestConfig = new RequestConfig();
        RequestConfig.Path path = new RequestConfig.Path();
        RequestConfig.Header header = new RequestConfig.Header();
        RequestConfig.Limit limit = new RequestConfig.Limit();
        RequestConfig.Token token = new RequestConfig.Token();
        RequestConfig.Record record = new RequestConfig.Record();
        RequestConfig.Record.Exclude exclude = new RequestConfig.Record.Exclude();
        exclude.setPath("/actuator");
        exclude.setIp("ip.elpsykongroo.com");
//        exclude.setIp("127.0.0.1");

        record.setExclude(exclude);
        token.setSpeed(10l);
        token.setDuration(1l);
        token.setTokens(10l);
        limit.setGlobal(token);
        path.setLimit("/");
        path.setFilter("/");
        path.setExclude("/actuator");
        path.setNonPrivate("/public");
        path.setPermit("/**");
        requestConfig.setPath(path);
        requestConfig.setHeader(header);
        requestConfig.setLimit(limit);
        requestConfig.setRecord(record);
        RestTemplateBuilderConfigurer configurer = new RestTemplateBuilderConfigurer();
        RestTemplateBuilder restTemplateBuilder =  configurer.configure(new RestTemplateBuilder())
                .rootUri("http://localhost:8880");
        RedisService redisService = new RedisServiceImpl("http://localhost:8880", restTemplateBuilder);
        SearchService searchService = new SearchServiceImpl("http://localhost:8880", restTemplateBuilder);
        IPManagerService ipManagerService = new IPMangerServiceImpl(requestConfig, redisService, searchService);
        AccessRecordService accessRecordService = new AccessRecordServiceImpl(searchService, ipManagerService, requestConfig);
        ThrottlingFilter filter = new ThrottlingFilter(requestConfig, accessRecordService, ipManagerService);
        filter.init(new MockFilterConfig(servletContext));
        request.setRequestURI("/public/ip");
        request.setMethod("GET");
        filter.doFilter(request, response, filterChain);
        filter.destroy();
//
//        request.setRequestURI("/ip/manage/list?black=&pageNumber=0&pageSize=10");
//        request.setMethod("GET");
    }
}
