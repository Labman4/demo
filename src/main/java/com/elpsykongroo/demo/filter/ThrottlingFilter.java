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

import com.elpsykongroo.demo.constant.Constant;
import com.elpsykongroo.demo.repo.IPRepo;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component("ThrottlingFilter")
@Slf4j
public class ThrottlingFilter implements Filter {
	private String errorMsg;
	private boolean limitFlag = false;
	private boolean publicFlag = true;
	private boolean blackFlag = false;

	@Value("${GLOBAL_REQUEST_LIMIT:1000}")
	private Long globalOverdraft;

	@Value("${GLOBAL_REQUEST_DURATION_SPEED:100}")
	private Long globalTokens;

	@Value("${GLOBAL_REQUEST_DURATION:1}")
	private Long globalDuration;
	@Value("${REQUEST_LIMIT:100}")
	private Long overdraft;
	@Value("${REQUEST_DURATION_SPEED:10}")
	private Long tokens;
	@Value("${REQUEST_DURATION:1}")
	private Long duration;
	@Value("${FILTER_PATH:/}")
	private String filtertPath;

	@Value("${EXCLUDE_PATH:/actuator}")
	private String excludePath;

	@Value("${LIMIT_PATH:/}")
	private String limitPath;
	@Value("${PUBLIC_PATH:/public}")
	private String publicPath;

	@Autowired
	private IPRepo ipRepo;
	@Autowired
	private AccessRecordService accessRecordService;
	@Autowired
	private IPManagerService ipMangerService;

	@Autowired
	private RedisTemplate redisTemplate;

	private Bucket createNewBucket() {
		Refill refill = Refill.greedy(tokens, Duration.ofSeconds(duration));
		Bandwidth limit = Bandwidth.classic(overdraft, refill);
		return Bucket.builder().addLimit(limit).build();
	}

	private Bucket createGlobalNewBucket() {
		Refill refill = Refill.greedy(globalTokens, Duration.ofSeconds(globalDuration));
		Bandwidth limit = Bandwidth.classic(globalDuration, refill);
		return Bucket.builder().addLimit(limit).build();
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
		HttpSession session = httpRequest.getSession(true);
		String requestUri = httpRequest.getRequestURI();
		errorMsg = "";
		if (StringUtils.isNotEmpty(limitPath) && PathUtils.beginWithPath(limitPath, requestUri)) {
			limitFlag = limitByBucket("global", limitPath, httpResponse, session, requestUri);
			accessRecordService.saveAcessRecord(httpRequest);
			if (limitFlag) {
				if (StringUtils.isNotEmpty(filtertPath) && PathUtils.beginWithPath(filtertPath, requestUri) && !PathUtils.beginWithPath(excludePath, requestUri)) {
					filterPath(filterChain, httpRequest, httpResponse, session, requestUri);
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

	private void filterPath(FilterChain filterChain, HttpServletRequest httpRequest, HttpServletResponse httpResponse, HttpSession session, String requestUri) throws UnknownHostException {
		if (limitByBucket("", filtertPath, httpResponse, session, requestUri)) {
			blackOrWhite(filterChain, httpRequest, httpResponse, requestUri);
		}
	}

	private void blackOrWhite(FilterChain filterChain, HttpServletRequest httpRequest, HttpServletResponse httpResponse, String requestUri) throws UnknownHostException {
		if (!ipMangerService.blackOrWhiteList(httpRequest, "true")) {
			blackFlag = false;
			publicFlag = isPublic(requestUri, httpRequest, httpResponse, filterChain);
		}
		else {
			blackFlag = true;
			httpResponse.setStatus(Constant.EMPTY_RESPONSE_CODE);
			httpResponse.setContentType("text/plain");
			errorMsg = ("yours IP is our blacklist");
		}
	}

	private boolean limitByBucket(String scope, String path, HttpServletResponse httpResponse, HttpSession session, String requestUri) {
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
				httpResponse.setStatus(Constant.LIMIT_RESPONSE_CODE);
				httpResponse.setHeader("X-Rate-Limit-Retry-After-Seconds", "" + TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
				httpResponse.setContentType("text/plain");
				errorMsg = "Too many requests";
				limitFlag = false;
				return false;
			}
		}
	}

	private boolean isPublic(String requestUri, ServletRequest request, HttpServletResponse servletResponse, FilterChain filterChain) throws UnknownHostException {
		if (!PathUtils.beginWithPath(publicPath, requestUri)) {
			if (!ipMangerService.blackOrWhiteList((HttpServletRequest) request, "false")) {
				servletResponse.setStatus(Constant.ACCESS_ERROR_CODE);
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
