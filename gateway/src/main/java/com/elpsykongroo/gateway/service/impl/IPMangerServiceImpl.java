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
import java.util.List;

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

	@Value("${service.whiteDomain:ip.elpsykongroo.com}")
	private String whiteDomain;

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
	public String list(String isBlack, String pageNumber, String pageSize) {
		return searchService.ipPageList(pageNumber, pageSize, isBlack);
	}

	@Override
	public void patch(String addresses, String isBlack, String id) throws UnknownHostException {
		String[] addr = addresses.split(",");
		if (StringUtils.isNotEmpty(id)) {
			searchService.deleteIpById(id);
			updateCache(isBlack);
		}
		for (String ad: addr) {
			InetAddress[] inetAddresses = InetAddress.getAllByName(ad);
			for (InetAddress inetAd: inetAddresses) {
				deleteIPManage(isBlack, inetAd.getHostAddress());
				deleteIPManage(isBlack, inetAd.getHostName());
			}
		}
		updateCache(isBlack);
	}

	private void deleteIPManage(String isBlack, String ad) {
		if ("true".equals(isBlack)) {
			searchService.deleteBlack(ad);
		}
		else {
			searchService.deleteWhite(ad);
		}
	}

	@Override
	public List<String> add(String addrs, String isBlack) throws UnknownHostException {
		List<String> addresses = new ArrayList<>();
//            RLock lock = redissonClient.getLock("blackList");
////            lock.tryLockAsync().get()
//            if (lock.tryLock(Constant.REDIS_LOCK_WAIT_TIME, Constant.REDIS_LOCK_LEASE_TIME, TimeUnit.SECONDS)) {
//                log.info("get lock");
//                try {
		if (StringUtils.isNotEmpty(addrs)) {
			String[] address = addrs.split(",");
			for (String addr: address) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(addr);
				for (InetAddress ad: inetAddresses) {
					if (exist(ad.getHostAddress(), isBlack) == 0) {
						addresses.add(searchService.saveIP(ad.getHostAddress(), isBlack));
					}
					if (exist(ad.getHostName(), isBlack) == 0) {
						addresses.add(searchService.saveIP(ad.getHostName(), isBlack));
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
		log.debug("black result------------:{}", addresses);
		updateCache(isBlack);
		return addresses;
	}

	private int exist(String ad, String isBlack) {
		String size = searchService.ipCount(ad, isBlack);
		log.debug("ip: {}, black: {}, size: {}", ad, isBlack, size);
		return Integer.parseInt(size);
	}

	private void updateCache(String isBlack) {
		String list = searchService.ipList(isBlack);
		redisService.set(env + isBlack, list, "");
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
				log.info("ip------------{}, type:{}, header:{}", ip, headerType, headers);
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

	public Boolean blackOrWhiteList(HttpServletRequest request, String isBlack){
		boolean flag = false;
		try {
			String list = redisService.get(env + isBlack);
			String ip = accessIP(request, isBlack);
			log.debug("cacheList: {}", list);
			if (StringUtils.isNotBlank(list)) {
				if ("false".equals(isBlack)) {
					log.debug("whiteDomain:{}", whiteDomain);
					for (String d : whiteDomain.split(",")) {
						if (!list.contains(d)) {
							initWhite();
						}
					}
				}
				if (list.contains(ip)) {
					return true;
				} else {
					for (String s : list.split(",")) {
						if (IPRegexUtils.vaildateHost(s)) {
							log.debug("query domain: {}", s);
							InetAddress[] inetAddress = InetAddress.getAllByName(s);
							for (InetAddress address : inetAddress) {
								if (address.getHostAddress().equals(ip)) {
									log.debug("update domain ip: {}", address.getHostAddress());
									add(address.getHostAddress(), isBlack);
									return true;
								}
							}
						}
					}
				}
			} else {
				log.info("updateCache");
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
//							String newAddress = ipRepo.save(new IPManage(address.getHostName(), Boolean.valueOf(isBlack)))
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
		} catch (UnknownHostException e) {
			log.error("UnknownHostException");
		} catch (FeignException e) {
			log.error("feign error :{}", e.getMessage());
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
				log.trace("ignore private ip");
				return inetAddress.isSiteLocalAddress();
			}
			if(IPRegexUtils.vaildateHost(ip)) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(ip);
				for (InetAddress addr: inetAddresses) {
					if (accessIP.equals(addr.getHostAddress())) {
						log.trace("host: {} match, accessIp:{}, ip:{}", ip, accessIP, addr.getHostAddress());
						return true;
					} else {
						log.trace("host: {} mismatch, accessIp:{}, ip:{}", ip, accessIP, addr.getHostAddress());
					}
				}			
			} else if (accessIP.equals(ip)) {
					log.trace("ip match, ip:{}, accessIp:{}", ip, accessIP);
					return true;
			}
		} catch (UnknownHostException e) {
			log.error("UnknownHostException");
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