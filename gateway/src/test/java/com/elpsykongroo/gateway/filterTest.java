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

import com.elpsykongroo.base.domain.search.repo.IpManage;
import com.elpsykongroo.base.utils.JsonUtils;
import com.elpsykongroo.gateway.config.RequestConfig;
import com.elpsykongroo.gateway.filter.ThrottlingFilter;
import com.elpsykongroo.gateway.service.AccessRecordService;
import com.elpsykongroo.gateway.service.IPManagerService;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.service.SearchService;
import com.elpsykongroo.gateway.service.impl.AccessRecordServiceImpl;
import com.elpsykongroo.gateway.service.impl.IPMangerServiceImpl;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.mockserver.model.MediaType;
import org.springframework.test.context.event.annotation.AfterTestClass;

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

    @BeforeEach
    public void setUp() {
        servletContext = new MockServletContext();
        request = new MockHttpServletRequest(servletContext);
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        mockServerClient = startClientAndServer(8880);
        IpManage ipManage = new IpManage();
        // place in beforeEach not work
        mockServerClient
                .when(request()
                        .withPath("/redis/key.*")
                        .withMethod("GET"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(""));
        mockServerClient
                .when(request()
                        .withPath("/redis/key")
                        .withMethod("PUT"))
                .respond(response()
                        .withStatusCode(200));
        mockServerClient.when(request().withPath("/search/ip").withMethod("PUT"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(JsonUtils.toJson(Collections.singleton(ipManage.getAddress())), MediaType.APPLICATION_JSON));
        mockServerClient.when(request().withPath("/search/ip").withMethod("GET"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody(JsonUtils.toJson(Collections.singleton(ipManage)), MediaType.APPLICATION_JSON));
        mockServerClient.when(request().withPath("/search/ip/count").withMethod("GET"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody("0", MediaType.APPLICATION_JSON));
    }

    @AfterTestClass
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
        RedisService redisService = Feign.builder()
                .decoder(new Decoder.Default())
                .encoder(new Encoder.Default())
                .target(RedisService.class, "http://localhost:8880");
        SearchService searchService = Feign.builder()
                .decoder(new Decoder.Default())
                .encoder(new Encoder.Default())
                .target(SearchService.class, "http://localhost:8880");
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
        IPManagerService ipManagerService = new IPMangerServiceImpl(requestConfig, redisService, searchService);
        AccessRecordService accessRecordService = new AccessRecordServiceImpl(ipManagerService, requestConfig);
        ThrottlingFilter filter = new ThrottlingFilter(requestConfig, accessRecordService, ipManagerService);
        filter.init(new MockFilterConfig(servletContext));
        request.setRequestURI("/public/ip");
        request.setMethod("GET");
        filter.doFilter(request, response, filterChain);
        filter.destroy();
    }
}
