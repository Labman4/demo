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

package com.elpsykongroo.gateway.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.elpsykongroo.base.domain.search.repo.IpManage;
import com.elpsykongroo.base.domain.search.QueryParam;
import com.elpsykongroo.base.utils.IPUtils;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.service.SearchService;
import com.elpsykongroo.base.utils.RecordUtils;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;

import com.elpsykongroo.base.config.RequestConfig;
import com.elpsykongroo.gateway.service.IPManagerService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IPMangerServiceImpl implements IPManagerService {
	@Value("${REDIS_KEY_PREFIX:dev}")
	private String env;

	@Value("${service.whiteDomain:ip.elpsykongroo.com,localhost}")
	private String whiteDomain = "localhost";

    @Autowired
	private SearchService searchService;

	@Autowired
	private RedisService redisService;

	public IPMangerServiceImpl(RequestConfig requestConfig,
							   RedisService redisService,
							   SearchService searchService) {
		this.requestConfig = requestConfig;
		this.redisService = redisService;
		this.searchService = searchService;
	}

	@Autowired
	private RequestConfig requestConfig;

	/*
	 * X-Forwarded-For
	 * Proxy-Client-IP
	 * WL-Proxy-Client-IP
	 * HTTP_CLIENT_IP
	 * HTTP_X_FORWARDED_FOR
	 *
	 * */

	@Override
	public String list(String isBlack, String pageNumber, String pageSize, String order) {
		try {
			QueryParam queryParam = new QueryParam();
			queryParam.setPageNumber(pageNumber);
			queryParam.setPageSize(pageSize);
			queryParam.setOrder(order);
			queryParam.setOrderBy("timestamp");
			queryParam.setType(IpManage.class);
			queryParam.setIndex("ip");
			queryParam.setParam(isBlack);
			queryParam.setField("black");
			return searchService.query(queryParam);
		} catch (FeignException e) {
			if(log.isErrorEnabled()) {
				log.error("feign error :{}", e.getMessage());
			}
			return "";
		}
	}

	@Override
	public String patch(List<String> address, String isBlack, String id) throws UnknownHostException {
		int updated = 0;
		String script = "ctx._source.black=params.black;";
		QueryParam queryParam = new QueryParam();
		Map<String, Object> update = new HashMap<>();
		queryParam.setIndex("ip");
		if (StringUtils.isNotEmpty(isBlack)) {
			update.put("black", isBlack);
		}
		try {
			if (StringUtils.isNotEmpty(id)) {
				queryParam.setOperation("update");
				queryParam.setIds(Collections.singletonList(id).stream().toList());
				queryParam.setUpdateParam(update);
				queryParam.setScript(script);
				String result = searchService.query(queryParam);
				updateCache(isBlack);
				return String.valueOf(result);
			}
			for (String ad : address) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(ad);
				for (InetAddress inetAd : inetAddresses) {
					List<String> params = new ArrayList<>();
					List<String> fields = new ArrayList<>();
					params.add(inetAd.getHostAddress());
					params.add(inetAd.getHostName());
					List<String> p = params.stream().distinct().collect(Collectors.toList());
					for (int i=0; i< p.size(); i++) {
						if (IPUtils.isIpv6(p.get(i))) {
							String np = "\"" + p.get(i) + "\"";
							p.remove(i);
							p.add(np);
						}
					}
					fields.add("address");
					queryParam.setBoolQuery(true);
					queryParam.setQueryStringParam(p);
					if (queryParam.getQueryStringParam().size() > 1) {
						fields.add("address");
					}
					queryParam.setFields(fields);
					if (StringUtils.isEmpty(isBlack)) {
						queryParam.setOperation("deleteQuery");
						queryParam.setType(IpManage.class);
						queryParam.setBoolType("should");
						String deleted = searchService.query(queryParam);
						updated += Integer.parseInt(deleted);
					} else if (exist(inetAd.getHostAddress(), isBlack) == 0){
						updated += add(Collections.singleton(inetAd.getHostAddress()).stream().toList(), isBlack);
					} else if (exist(inetAd.getHostName(), isBlack) == 0) {
						updated += add(Collections.singleton(inetAd.getHostName()).stream().toList(), isBlack);
					} else {
						queryParam.setOperation("updateQuery");
						queryParam.setUpdateParam(update);
						queryParam.setBoolType("should");
						queryParam.setType(IpManage.class);
						queryParam.setScript(script);
						String u = searchService.query(queryParam);
						updated += Integer.parseInt(u);
					}
				}
			}
		} catch (FeignException e) {
			if(log.isErrorEnabled()) {
				log.error("feign error :{}", e.getMessage());
			}
		}
		updateCache(isBlack);
		return String.valueOf(updated);
	}

	@Override
	public int add(List<String> addresses, String isBlack) throws UnknownHostException {
		int result = 0;
		if (log.isDebugEnabled()) {
			log.debug("add ip:{}, black:{}", addresses, isBlack);
		}
		if (StringUtils.isEmpty(isBlack)) {
			return 0;
		}
		if (!addresses.isEmpty()) {
			QueryParam queryParam = new QueryParam();
			queryParam.setIndex("ip");
			queryParam.setOperation("save");
			for (String address: addresses) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(address);
				for (InetAddress ad: inetAddresses) {
					if(addNoExist(isBlack, queryParam, ad.getHostAddress())) {
						result++;
					}
					if (!ad.getHostAddress().equals(ad.getHostName())) {
						if(addNoExist(isBlack, queryParam, ad.getHostName())) {
							result++;
						}
					}
				}
			}
		}
		updateCache(isBlack);
		return result;
	}

	private boolean addNoExist(String isBlack, QueryParam queryParam, String ad) {
		int size = exist(ad, isBlack);
		if(log.isDebugEnabled()) {
			log.debug("exist size :{}", size);
		}
		try {
			if (size == 0) {
				queryParam.setEntity(new IpManage(ad, isBlack));
				searchService.query(queryParam);
				return true;
			}
		} catch (FeignException e) {
			if(log.isErrorEnabled()) {
				log.error("feign error :{}", e.getMessage());
			}
		}
		return false;
	}

	private int exist(String ad, String isBlack) {
		String count = null;
		try {
			QueryParam queryParam = new QueryParam();
			List<String> fields = new ArrayList<>();
			fields.add("address");
			fields.add("black");
			List<String> params = new ArrayList<>();
			if (IPUtils.isIpv6(ad)) {
				params.add("\"" + ad + "\"");
			} else {
				params.add(ad);
			}
			params.add(isBlack);
			queryParam.setQueryStringParam(params);
			queryParam.setFields(fields);
			queryParam.setBoolQuery(true);
			queryParam.setType(IpManage.class);
			queryParam.setIndex("ip");
			queryParam.setOperation("count");
			count = searchService.query(queryParam);
			if (log.isDebugEnabled()) {
				log.debug("ip: {}, black: {}, size: {}", ad, isBlack, count);
			}
		} catch (FeignException e) {
			if(log.isErrorEnabled()) {
				log.error("feign error :{}", e.getMessage());
			}
		}
		return StringUtils.isNotBlank(count) ? Integer.parseInt(count) : 0;
	}

	private void updateCache(String isBlack) {
		try {
			String ipList = getIpList(isBlack);
			redisService.set(env + isBlack, ipList, "");
		} catch (FeignException e) {
			if(log.isErrorEnabled()) {
				log.error("feign error :{}", e.getMessage());
			}
		}
	}

	private String getIpList(String isBlack) {
		QueryParam queryParam = new QueryParam();
		queryParam.setIndex("ip");
		queryParam.setType(IpManage.class);
		queryParam.setField("black");
		queryParam.setParam(isBlack);
		String list = searchService.query(queryParam);
//		List<IpManage> ipManages = JsonUtils.toType(list, new TypeReference<List<IpManage>>() {});
//		for (IpManage ipManage : ipManages) {
//
//		}
		StringBuffer ipList = new StringBuffer();
		String[] ips = list.split(",");
		for (String str: ips) {
			if(str.contains("address")) {
				ipList.append(str.split("=")[1]).append(",");
			}
		}
		return ipList.toString();
	}

	@Override
	public String accessIP(HttpServletRequest request, String headerType) {
		IPUtils ipUtils = new IPUtils(requestConfig);
		String ip = ipUtils.accessIP(request, "");
		RecordUtils recordUtils = new RecordUtils(requestConfig);
		if (recordUtils.filterRecord(request)) {
			if (log.isInfoEnabled()) {
				log.info("ip------------{}, type:{}", ip, headerType);
			}
		}
		return ip;
	}

	@Override
	public Boolean blackOrWhiteList(HttpServletRequest request, String isBlack, String ip){
		boolean flag = false;
		try {
			if (StringUtils.isBlank(ip)) {
				ip = accessIP(request, isBlack);
			}
			if(log.isWarnEnabled()) {
				log.warn("blackOrWhiteList ip:{}, black:{}", ip, isBlack);
			}
			if (IPUtils.isPrivate(ip)) {
				if(log.isWarnEnabled()) {
					log.warn("ignore private ip:{}", ip);
				}
				return !Boolean.valueOf(isBlack);
			}
			String list = "";
			try {
				list = redisService.get(env + isBlack);
				if (log.isInfoEnabled()) {
					log.info("cacheList:{}, black:{}", list, isBlack);
				}
				if (StringUtils.isBlank(list)) {
					list = getIpList(isBlack);
				}
				if (log.isInfoEnabled()) {
					log.info("ipList:{}, black:{}", list, isBlack);
				}
			} catch (FeignException e) {
				if(log.isErrorEnabled()) {
					log.error("feign error :{}", e.getMessage());
				}
			}
			if (StringUtils.isNotBlank(list)) {
				if ("false".equals(isBlack)) {
					if (log.isDebugEnabled()) {
						log.debug("whiteDomain:{}", whiteDomain);
					}
					for (String d : whiteDomain.split(",")) {
						InetAddress[] inetAddress = InetAddress.getAllByName(d);
						for (InetAddress address : inetAddress) {
							if (!list.contains(address.getHostAddress())) {
								if (log.isWarnEnabled()) {
									log.warn("out of white:{}", address.getHostAddress());
								}
								initWhite();
							}
						}
					}
				}
				if (list.contains(ip)) {
					flag = true;
				} else {
					if (log.isWarnEnabled()) {
						log.warn("try to query domain in cacheList");
					}
					for (String s: list.split(",")) {
						if (log.isWarnEnabled()) {
							log.warn("try to query domain: {}", s);
						}
						if (IPUtils.validateHost(s)) {
							InetAddress[] inetAddress = null;
							try {
								inetAddress = InetAddress.getAllByName(s);
							} catch (UnknownHostException e) {
								continue;
							}
							for (InetAddress address : inetAddress) {
								if (log.isWarnEnabled()) {
									log.warn("try to update domain: {}", address);
								}
								if (address.getHostAddress().equals(ip)) {
									if (log.isWarnEnabled()) {
										log.warn("update domain ip: {}", address.getHostAddress());
									}
									add(Collections.singleton(address.getHostAddress()).stream().toList(), isBlack);
									flag = true;
								}
							}
						}
					}
				}
			} else {
				if(log.isWarnEnabled()) {
					log.warn("updateCache");
				}
				if ("false".equals(isBlack)) {
					initWhite();
				}
				updateCache(isBlack);
			}
			/**
			 * 	solved
			 *
			 * 	reserve dns need ptr record and public static ip;
			 *  cannot get hostname; need to search first;
			 *  if exist too many domain record in es may cause problem;
			 *
			 *  solved
			 *    query all domain in cache when request don't match cache
			 */
			if (exist(ip, isBlack) > 0) {
				flag = true;
			}
			log.debug("flag:{}, black:{}", flag, isBlack);
		} catch (UnknownHostException e) {
			if (log.isErrorEnabled()) {
				log.error("UnknownHostException");
			}
		}
		return flag;
    }

	private void initWhite(){
		try {
			for(String d: whiteDomain.split(",")) {
				add(Collections.singleton(d).stream().toList(), "false");
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
}