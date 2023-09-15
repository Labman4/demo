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

package com.elpsykongroo.gateway.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.elpsykongroo.base.utils.PathUtils;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.elpsykongroo.base.config.RequestConfig;
import com.elpsykongroo.gateway.service.AccessRecordService;
import com.elpsykongroo.gateway.service.IPManagerService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component("ThrottlingFilter")
@Slf4j
public class ThrottlingFilter implements Filter {
	private ThreadLocal<String> errorMsg = new ThreadLocal<>();
	@Autowired
	private RequestConfig requestConfig;

	@Autowired
	private AccessRecordService accessRecordService;

	@Autowired
	private IPManagerService ipMangerService;

	public ThrottlingFilter(RequestConfig requestConfig,
							AccessRecordService accessRecordService,
							IPManagerService ipMangerService) {
		this.requestConfig = requestConfig;
		this.accessRecordService = accessRecordService;
		this.ipMangerService = ipMangerService;
	}

	private Bucket createBucket(Long speed, Long duration, Long tokens) {
		Refill refill = Refill.greedy(speed,
				Duration.ofSeconds(duration));
		Bandwidth limit = Bandwidth.classic(tokens, refill);
		return Bucket.builder().addLimit(limit).build();
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		if (log.isTraceEnabled()) {
			log.trace("request filter path:{}", requestConfig.getPath());
		}
		HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
		HttpSession session = httpRequest.getSession(true);
		String requestUri = httpRequest.getRequestURI();
		errorMsg.set("");
		String limitPath = requestConfig.getPath().getLimit();
		if (StringUtils.isNotEmpty(limitPath) && PathUtils.beginWithPath(limitPath, requestUri)) {
			accessRecordService.saveAccessRecord(httpRequest);
			String filterPath = requestConfig.getPath().getFilter();
			String excludePath = requestConfig.getPath().getExclude();
			if (limitByBucket("global", httpResponse, session)) {
				if (StringUtils.isNotEmpty(filterPath)
						&& PathUtils.beginWithPath(filterPath, requestUri)
						&& !PathUtils.beginWithPath(excludePath, requestUri)) {
					log.debug("start filter:{}", requestUri);
					if (filterPath(httpRequest, httpResponse, session, requestUri)) {
						filterChain.doFilter(servletRequest, servletResponse);
					} else {
						httpResponse.getWriter().append(errorMsg.get());
					}
				} else {
					filterChain.doFilter(servletRequest, servletResponse);
				}
			}
		}
	}

	private boolean filterPath(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpSession session, String requestUri){
		if (limitByBucket("", httpResponse, session)) {
			if (blackOrWhite(httpRequest, httpResponse)) {
				return isPublic(requestUri, httpRequest, httpResponse);
			}
		}
		return false;
	}

	private boolean blackOrWhite(HttpServletRequest httpRequest, HttpServletResponse httpResponse){
		if (ipMangerService.blackOrWhiteList(httpRequest, "false", "")) {
			return true;
		} else if (!ipMangerService.blackOrWhiteList(httpRequest, "true", "")) {
			return true;
		} else {
			errorMsg.set("yours IP is our blacklist");
			httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
			httpResponse.setContentType("text/plain");
			return false;
		}
	}

	private boolean limitByBucket(String scope, HttpServletResponse httpResponse, HttpSession session) {
		String attrName = "";
		if ("global".equals(scope)) {
			attrName = scope + "throttler";
		} else {
			attrName = scope + "throttler" + session.getId();
		}
		Bucket bucket = (Bucket) session.getAttribute(attrName);
		if (bucket == null) {
			if ("global".equals(scope)) {
				bucket = createBucket(
						requestConfig.getLimit().getGlobal().getSpeed(),
						requestConfig.getLimit().getGlobal().getDuration(),
						requestConfig.getLimit().getGlobal().getTokens());
			} else {
				bucket = createBucket(
						requestConfig.getLimit().getScope().getSpeed(),
						requestConfig.getLimit().getScope().getDuration(),
						requestConfig.getLimit().getScope().getTokens());
			}
			session.setAttribute(attrName, bucket);
		}
		// tryConsume returns false immediately if no tokens available with the bucket
		if (bucket.tryConsume(1)) {
			// the limit is not exceeded
			return true;
//				filterChain.doFilter(servletRequest, servletResponse);
		} else {
			// limit is exceeded
			ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
			if (probe.isConsumed()) {
				// the limit is not exceeded
				httpResponse.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
				return true;
			}
			else {
				// limit is exceeded
				httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
				httpResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill())));
				httpResponse.setContentType("text/plain");
				errorMsg.set("Too many requests");
				return false;
			}
		}
	}

	private boolean isPublic(String requestUri, ServletRequest request, HttpServletResponse servletResponse) {
		if (!PathUtils.beginWithPath(requestConfig.getPath().getNonPrivate(), requestUri)) {
			if (!ipMangerService.blackOrWhiteList((HttpServletRequest) request, "false", "")) {
				servletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
				servletResponse.setContentType("text/plain");
				errorMsg.set("no access");
				return false;
			}
		}
		return true;
	}
}
