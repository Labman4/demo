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

package com.elpsykongroo.demo.filter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.elpsykongroo.demo.config.RequestConfig;
import com.elpsykongroo.demo.service.AccessRecordService;
import com.elpsykongroo.demo.service.IPManagerService;
import com.elpsykongroo.demo.utils.PathUtils;
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
	private String errorMsg;
	private boolean limitFlag = false;
	private boolean publicFlag = true;
	private boolean blackFlag = false;

	@Autowired
	private RequestConfig requestConfig;

	@Autowired
	private AccessRecordService accessRecordService;

	@Autowired
	private IPManagerService ipMangerService;

	private Bucket createNewBucket() {
		Refill refill = Refill.greedy(requestConfig.getLimit().getScope().getSpeed(),
					    Duration.ofSeconds(requestConfig.getLimit().getScope().getDuration()));
		Bandwidth limit = Bandwidth.classic(requestConfig.getLimit().getScope().getTokens(), refill);
		return Bucket.builder().addLimit(limit).build();
	}

	private Bucket createGlobalNewBucket() {
		Refill refill = Refill.greedy(requestConfig.getLimit().getGlobal().getSpeed(), 
						Duration.ofSeconds(requestConfig.getLimit().getGlobal().getDuration()));
		Bandwidth limit = Bandwidth.classic(requestConfig.getLimit().getGlobal().getTokens(), refill);
		return Bucket.builder().addLimit(limit).build();
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
		HttpSession session = httpRequest.getSession(true);
		String requestUri = httpRequest.getRequestURI();
		errorMsg = "";
		String limitPath = requestConfig.getPath().getLimit();
		if (StringUtils.isNotEmpty(limitPath) && PathUtils.beginWithPath(limitPath, requestUri)) {
			limitFlag = limitByBucket("global", httpResponse, session);
			accessRecordService.saveAcessRecord(httpRequest);
			String filtertPath = requestConfig.getPath().getFilter();
			String excludePath = requestConfig.getPath().getExclude().getAll();
			if (limitFlag) {
				if (StringUtils.isNotEmpty(filtertPath)
						&& PathUtils.beginWithPath(filtertPath, requestUri)
						&& !PathUtils.beginWithPath(excludePath, requestUri)) {
					log.info("start filter:{}", requestUri);
					filterPath(httpRequest, httpResponse, session, requestUri);
				}
			}
		}
		if (!limitFlag || !publicFlag || blackFlag) {
			httpResponse.getWriter().append(errorMsg);
		}
		else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	private void filterPath(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpSession session, String requestUri) throws UnknownHostException {
		if (limitByBucket("", httpResponse, session)) {
			blackOrWhite(httpRequest, httpResponse, requestUri);
		}
	}

	private void blackOrWhite(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String requestUri) throws UnknownHostException {
		if (!ipMangerService.blackOrWhiteList(httpRequest, "true")) {
			blackFlag = false;
			publicFlag = isPublic(requestUri, httpRequest, httpResponse);
		}
		else {
			blackFlag = true;
			errorMsg = "yours IP is our blacklist";
			httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
			httpResponse.setContentType("text/plain");
		}
	}

	private boolean limitByBucket(String scope, HttpServletResponse httpResponse, HttpSession session) {
		String appKey = session.getId();
		Bucket bucket = (Bucket) session.getAttribute("throttler-" + appKey);
		if (bucket == null) {
			if ("global".equals(scope)) {
				bucket = createGlobalNewBucket();
			}
			else {
				bucket = createNewBucket();
			}
			session.setAttribute("throttler-" + appKey, bucket);
		}
		// tryConsume returns false immediately if no tokens available with the bucket
		if (bucket.tryConsume(1)) {
			// the limit is not exceeded
			return true;
//				filterChain.doFilter(servletRequest, servletResponse);
		}
		else {
			// limit is exceeded
			ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
			if (probe.isConsumed()) {
				// the limit is not exceeded
				httpResponse.setHeader("X-Rate-Limit-Remaining", "" + probe.getRemainingTokens());
				return true;
			}
			else {
				// limit is exceeded
				httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
				httpResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "" + TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
				httpResponse.setContentType("text/plain");
				errorMsg = "Too many requests";
				limitFlag = false;
				return false;
			}
		}
	}

	private boolean isPublic(String requestUri, ServletRequest request, HttpServletResponse servletResponse) throws UnknownHostException {
		if (!PathUtils.beginWithPath(requestConfig.getPath().getNonPrivate(), requestUri)) {
			if (!ipMangerService.blackOrWhiteList((HttpServletRequest) request, "false")) {
				servletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
				servletResponse.setContentType("text/plain");
				errorMsg = "no access";
				return false;
			}
			else {
				return true;
			}
		}
		else {
			return true;
		}
	}
}
