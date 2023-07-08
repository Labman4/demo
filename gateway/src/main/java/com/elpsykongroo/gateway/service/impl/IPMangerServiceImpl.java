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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.elpsykongroo.base.domain.search.repo.IpManage;
import com.elpsykongroo.base.domain.search.QueryParam;
import com.elpsykongroo.base.utils.IPRegexUtils;
import com.elpsykongroo.base.utils.PathUtils;
import com.elpsykongroo.base.service.RedisService;
import com.elpsykongroo.base.service.SearchService;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;

import com.elpsykongroo.gateway.config.RequestConfig;
import com.elpsykongroo.gateway.config.RequestConfig.Header;
import com.elpsykongroo.gateway.config.RequestConfig.Record.Exclude;
import com.elpsykongroo.gateway.service.IPManagerService;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IPMangerServiceImpl implements IPManagerService {
	@Value("${ENV:dev}")
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
	public String patch(String address, String isBlack, String id) throws UnknownHostException {
		long updated = 0;
		String script = "ctx._source.black=params.black;";
		String[] addr = address.split(",");
		QueryParam queryParam = new QueryParam();
		Map<String, Object> update = new HashMap<>();
		queryParam.setIndex("ip");
		if (StringUtils.isNotEmpty(isBlack)) {
			update.put("black", isBlack);
		}
		try {
			if (StringUtils.isNotEmpty(id)) {
				queryParam.setOperation("update");
				queryParam.setIds(id);
				queryParam.setUpdateParam(update);
				queryParam.setScript(script);
				String result = searchService.query(queryParam);
				updateCache(isBlack);
				return String.valueOf(result);
			}
			for (String ad : addr) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(ad);
				for (InetAddress inetAd : inetAddresses) {
					List<String> params = new ArrayList<>();
					List<String> fields = new ArrayList<>();
					params.add(inetAd.getHostAddress());
					params.add(inetAd.getHostName());
					fields.add("address");
					queryParam.setBoolQuery(true);
					queryParam.setQueryStringParam(params.stream().distinct().collect(Collectors.toList()));
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
	public String add(String addrs, String isBlack) throws UnknownHostException {
		long result = 0;
//            RLock lock = redissonClient.getLock("blackList");
////            lock.tryLockAsync().get()
//            if (lock.tryLock(Constant.REDIS_LOCK_WAIT_TIME, Constant.REDIS_LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
//                log.info("get lock");
//                try {
		if (log.isDebugEnabled()) {
			log.debug("add ip:{}, black:{}", addrs, isBlack);
		}
		if (StringUtils.isNotEmpty(addrs)) {
			QueryParam queryParam = new QueryParam();
			queryParam.setIndex("ip");
			queryParam.setOperation("save");
			String[] address = addrs.split(",");
			for (String addr: address) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(addr);
				for (InetAddress ad: inetAddresses) {
					if(addNoExist(isBlack, queryParam, ad.getHostAddress())) {
						result++;
					};
					if (!ad.getHostAddress().equals(ad.getHostName())) {
						if(addNoExist(isBlack, queryParam, ad.getHostName())) {
							result++;
						};
					}
				}
			}
		}
//                } finally {
//                    lock.unlock();
//                }
//            }else {
//                log.info("wait for lock");
//                addresses.add("failed ");
//            }
//        } catch (InterruptedException e) {
//            return commonResponse.error(Constant.ERROR_CODE, "please retry");
		updateCache(isBlack);
		return String.valueOf(result);
	}

	private boolean addNoExist(String isBlack, QueryParam queryParam, String ad) {
		int size = exist(ad, isBlack);
		if(log.isDebugEnabled()) {
			log.debug("exist size :{}", size);
		}
		try {
			if ( size == 0) {
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
			params.add(ad);
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
			QueryParam queryParam = new QueryParam();
			queryParam.setIndex("ip");
			queryParam.setType(IpManage.class);
			queryParam.setField("black");
			queryParam.setParam(isBlack);
			String list = searchService.query(queryParam);
			StringBuffer ipList = new StringBuffer();
			String[] ips = list.split(",");
			for (String str: ips) {
				if(str.contains("address")) {
					ipList.append(str.split("=")[1]).append(",");
				}
			}
			redisService.set(env + isBlack, ipList.toString(), "");
		} catch (FeignException e) {
			if(log.isErrorEnabled()) {
				log.error("feign error :{}", e.getMessage());
			}
		}
	}

	@Override
	public String accessIP(HttpServletRequest request, String headerType) {		
		String[] headers = splitHeader(headerType);
		String ip = getIp(request, headers);
		Exclude recordExclude = requestConfig.getRecord().getExclude();		
		if (!PathUtils.beginWithPath(recordExclude.getPath(), request.getRequestURI())) {
			String[] head= splitHeader("record");
			String recordIp = getIp(request, head);                                                       
			boolean recordFlag = filterByIpOrList(recordExclude.getIp(), recordIp);
			if (!recordFlag) {
				if (log.isInfoEnabled()) {
					log.info("ip------------{}, type:{}, header:{}", ip, headerType, headers);
				}
			}
		}
		return ip;
	}

	private String[] splitHeader (String headerType) {
		Header header = requestConfig.getHeader();
		switch(headerType){
			case "true":
				return header.getBlack().split(",");
			case "false":
				return header.getWhite().split(",");
			case "record":
				return header.getRecord().split(",");
			default:
				return header.getIp().split(",");
		}
	}

	private String getIp(HttpServletRequest request, String[] headers) {
		String ip = "";
		for (String head: headers) {
			ip = request.getHeader(head);
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				continue;
			}
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}

	@Override
	public Boolean blackOrWhiteList(HttpServletRequest request, String isBlack){
		boolean flag = false;
		try {
			String list = "";
			try {
				list = redisService.get(env + isBlack);
			} catch (FeignException e) {
				if(log.isErrorEnabled()) {
					log.error("feign error :{}", e.getMessage());
				}
			}
			String ip = accessIP(request, isBlack);
			if (log.isDebugEnabled()) {
				log.debug("cacheList: {}, black: {}", list, isBlack);
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
					return true;
				} else {
					for (String s : list.split(",")) {
						if (IPRegexUtils.vaildateHost(s)) {
							if (log.isDebugEnabled()) {
								log.debug("query domain: {}", s);
							}
							InetAddress[] inetAddress = new InetAddress[0];
							try {
								inetAddress = InetAddress.getAllByName(s);
							} catch (UnknownHostException e) {
								continue;
							}
							for (InetAddress address : inetAddress) {
								if (address.getHostAddress().equals(ip)) {
									if (log.isDebugEnabled()) {
										log.debug("update domain ip: {}", address.getHostAddress());
									}
									add(address.getHostAddress(), isBlack);
									return true;
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
//			InetAddress[] inetAddress = InetAddress.getAllByName(ip);
			if (exist(ip, isBlack) > 0) {
				flag = true;
				// not work address.getHostName() only return ipaddress without ptr
//					for (InetAddress address: inetAddress) {
//						if (exist(address.getHostName(), isBlack) == 0) {
//							String newAddress = ipRepo.save(new IpManage(address.getHostName(), Boolean.valueOf(isBlack)))
//									.getAddress();
//							updataCache(isBlack);
//							log.info("Update list domain when IP domain change, {} -> {}", ip, newAddress);
//						}
//					}
			}
//				else {
//					for (InetAddress address: inetAddress) {
//						if (exist(address.getHostName(), isBlack) > 0) {
//							log.info("hostname in list");
//							flag = true;
//						}
//					}
//				}
			log.debug("flag:{}, black:{}", flag, isBlack);
			if ("0:0:0:0:0:0:0:1".equals(request.getRemoteAddr()) ||
					"127.0.0.1".equals(request.getRemoteAddr())) {
				if(log.isTraceEnabled()) {
					log.trace("ignore private ip");
				}
				return true;
			}
		} catch (UnknownHostException e) {
			if (log.isErrorEnabled()) {
				log.error("UnknownHostException");
			}
		}
		return flag;
    }

	@Override
	public boolean filterByIpOrList(String ip, String accessIP) {
		if (StringUtils.isNotEmpty(ip)) {
			String[] ips = ip.split(",");
			for (String i: ips) {
				if(filterByIp(i, accessIP)){
					return true;
				}
			}
		}
		return false;
	} 

	@Override
	public boolean filterByIp(String ip, String accessIP) {
		try {
			InetAddress inetAddress = InetAddress.getByName(accessIP);
			if (inetAddress.isSiteLocalAddress()) {
				if(log.isTraceEnabled()) {
					log.trace("ignore private ip");
				}
				return inetAddress.isSiteLocalAddress();
			}
			if(IPRegexUtils.vaildateHost(ip)) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(ip);
				for (InetAddress addr: inetAddresses) {
					if (accessIP.equals(addr.getHostAddress())) {
						if(log.isTraceEnabled()) {
							log.trace("host: {} match, accessIp:{}, ip:{}", ip, accessIP, addr.getHostAddress());
						}
						return true;
					} else {
						if(log.isTraceEnabled()) {
							log.trace("host: {} mismatch, accessIp:{}, ip:{}", ip, accessIP, addr.getHostAddress());
						}
					}
				}			
			} else if (accessIP.equals(ip)) {
				if(log.isTraceEnabled()) {
					log.trace("ip match, ip:{}, accessIp:{}", ip, accessIP);
				}
				return true;
			}
		} catch (UnknownHostException e) {
			if (log.isErrorEnabled()) {
				log.error("UnknownHostException");
			}
		}
		return false;
	}

	private void initWhite(){
		try {
			for(String d: whiteDomain.split(",")) {
				add(d, "false");
			}
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}
}