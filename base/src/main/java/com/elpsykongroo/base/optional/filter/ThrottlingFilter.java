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

package com.elpsykongroo.base.optional.filter;

import com.elpsykongroo.base.config.RequestConfig;
import com.elpsykongroo.base.service.GatewayService;
import com.elpsykongroo.base.utils.IPUtils;
import com.elpsykongroo.base.utils.PathUtils;
import com.elpsykongroo.base.utils.RecordUtils;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ThrottlingFilter implements Filter {
	private ThreadLocal<String> errorMsg = new ThreadLocal<>();

	private RequestConfig requestConfig;

	private GatewayService gatewayService;

	public ThrottlingFilter(RequestConfig requestConfig, GatewayService gatewayService) {
		this.requestConfig = requestConfig;
		this.gatewayService =gatewayService;
	}

	private Bucket createNewBucket() {
		if (log.isTraceEnabled()) {
			log.trace("request scope limit:{}", requestConfig.getLimit().getScope());
		}
		Refill refill = Refill.greedy(requestConfig.getLimit().getScope().getSpeed(),
					    Duration.ofSeconds(requestConfig.getLimit().getScope().getDuration()));
		Bandwidth limit = Bandwidth.classic(requestConfig.getLimit().getScope().getTokens(), refill);
		return Bucket.builder().addLimit(limit).build();
	}

	private Bucket createGlobalNewBucket() {
		if (log.isTraceEnabled()) {
			log.trace("request global limit:{}", requestConfig.getLimit().getGlobal());
		}
		Refill refill = Refill.greedy(requestConfig.getLimit().getGlobal().getSpeed(), 
						Duration.ofSeconds(requestConfig.getLimit().getGlobal().getDuration()));
		Bandwidth limit = Bandwidth.classic(requestConfig.getLimit().getGlobal().getTokens(), refill);
		return Bucket.builder().addLimit(limit).build();
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		if (log.isDebugEnabled()) {
			log.debug("request filter path:{}", requestConfig.getPath());
		}
		HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
		HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
		HttpSession session = httpRequest.getSession(true);
		String requestUri = httpRequest.getRequestURI();
		errorMsg.set("");
		String limitPath = requestConfig.getPath().getLimit();
		if (StringUtils.isNotEmpty(limitPath) && PathUtils.beginWithPath(limitPath, requestUri)) {
			IPUtils ipUtils = new IPUtils(requestConfig);
			RecordUtils recordUtils = new RecordUtils(gatewayService);
			recordUtils.saveRecord(httpRequest, ipUtils.accessIP(httpRequest, ""));
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

	private boolean filterPath(HttpServletRequest request, HttpServletResponse response, HttpSession session, String requestUri){
		try {
			if (limitByBucket("", response, session)) {
				IPUtils ipUtils = new IPUtils(requestConfig);
				if (blackOrWhite(request, response, ipUtils)) {
					return isPublic(requestUri, request, response, ipUtils);
				}
			}
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("blackOrWhite error:{}", e.getMessage());
			}
		}
		return false;
	}

	private boolean blackOrWhite(HttpServletRequest request, HttpServletResponse httpResponse, IPUtils ipUtils){
			if ("true".equals(gatewayService.blackOrWhite("false", ipUtils.accessIP(request, "false")))) {
				return true;
			} else if (!"true".equals(gatewayService.blackOrWhite("true", ipUtils.accessIP(request, "true")))) {
				return true;
			} else {
				errorMsg.set("yours IP is our blacklist");
				httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
				httpResponse.setContentType("text/plain");
				return false;
			}

	}

	private boolean limitByBucket(String scope, HttpServletResponse httpResponse, HttpSession session) {
		String appKey = session.getId();
		Bucket bucket = (Bucket) session.getAttribute(scope + "throttler-" + appKey);
		if (bucket == null) {
			if ("global".equals(scope)) {
				bucket = createGlobalNewBucket();
			} else {
				bucket = createNewBucket();
			}
			session.setAttribute(scope +"throttler-" + appKey, bucket);
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

	private boolean isPublic(String requestUri, HttpServletRequest request, HttpServletResponse servletResponse, IPUtils ipUtils) {
		if (!PathUtils.beginWithPath(requestConfig.getPath().getNonPrivate(), requestUri)) {
			if (!"true".equals(gatewayService.blackOrWhite( "false", ipUtils.accessIP(request, "false")))) {
				servletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
				servletResponse.setContentType("text/plain");
				errorMsg.set("no access");
				return false;
			}
		}
		return true;
	}
}
