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

import com.elpsykongroo.gateway.config.RequestConfig;
import com.elpsykongroo.gateway.filter.ThrottlingFilter;
import com.elpsykongroo.gateway.service.AccessRecordService;
import com.elpsykongroo.gateway.service.IPManagerService;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.service.SearchService;
import com.elpsykongroo.gateway.service.impl.AccessRecordServiceImpl;
import com.elpsykongroo.gateway.service.impl.IPMangerServiceImpl;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.StringDecoder;
import feign.jackson.JacksonEncoder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.event.annotation.AfterTestClass;

import java.io.IOException;

class FilterTest {
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
        mockServerClient = ClientAndServer.startClientAndServer(8880);
        // place in beforeEach not work
        mockServerClient
                .when(HttpRequest.request()
                        .withPath("/redis/key.*")
                        .withMethod("GET"))
                .respond(HttpResponse.response()
                        .withStatusCode(200)
                        .withBody("localhost"));
        mockServerClient
                .when(HttpRequest.request()
                        .withPath("/redis/key")
                        .withMethod("PUT"))
                .respond(HttpResponse.response()
                        .withStatusCode(200));
        mockServerClient.when(HttpRequest.request().withPath("/search.*").withMethod("POST"))
                .respond(HttpResponse.response()
                        .withStatusCode(200));
    }

    @AfterTestClass
    public void tearDown() {
        mockServerClient.stop();
    }

    @Test
    @Order(1)
    void filter() throws ServletException, IOException {
        RequestConfig requestConfig = new RequestConfig();
        RequestConfig.Path path = new RequestConfig.Path();
        RequestConfig.Header header = new RequestConfig.Header();
        RequestConfig.Limit limit = new RequestConfig.Limit();
        RequestConfig.Token token = new RequestConfig.Token();
        RequestConfig.Record record = new RequestConfig.Record();
        RequestConfig.Record.Exclude exclude = new RequestConfig.Record.Exclude();
        exclude.setPath("/actuator");
        exclude.setIp("ip.elpsykongroo.com");
        RedisService redisService = Feign.builder()
                .decoder(new Decoder.Default())
                .encoder(new Encoder.Default())
                .target(RedisService.class, "http://localhost:8880");
        SearchService searchService = Feign.builder()
                .decoder(new StringDecoder())
                .encoder(new JacksonEncoder(new ObjectMapper().registerModule(new JavaTimeModule()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)))
                .target(SearchService.class, "http://localhost:8880");
        record.setExclude(exclude);
        token.setSpeed(1l);
        token.setDuration(10l);
        token.setTokens(1l);
        limit.setScope(token);
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
        request.addHeader("1", "1");
        request.setRequestURI("/ip");
        request.setMethod("GET");
        filter.doFilter(request, response, filterChain);
        request.setRequestURI("/actuator");
        request.setMethod("GET");
        filter.doFilter(request, response, filterChain);
        limit.setGlobal(token);
    }
}
