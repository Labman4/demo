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

package com.elpsykongroo.demo.service.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.elpsykongroo.demo.exception.ServiceException;
import com.elpsykongroo.services.redis.client.RedisService;
import com.elpsykongroo.services.redis.client.dto.KV;
import jakarta.servlet.http.HttpServletRequest;

import com.elpsykongroo.demo.common.CommonResponse;
import com.elpsykongroo.demo.config.RequestConfig;
import com.elpsykongroo.demo.config.RequestConfig.Header;
import com.elpsykongroo.demo.config.RequestConfig.Record.Exclude;
import com.elpsykongroo.demo.domain.IPManage;
import com.elpsykongroo.demo.repo.elasticsearch.IPRepo;
import com.elpsykongroo.demo.service.IPManagerService;  
import com.elpsykongroo.demo.utils.IPRegexUtils;
import com.elpsykongroo.demo.utils.PathUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IPMangerServiceImpl implements IPManagerService {
	@Autowired
	private IPRepo ipRepo;

	@Autowired
	private RedisService redisService;

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
	public CommonResponse<List<IPManage>> list(String isBlack, String pageNumber, String pageSize) {
		List<IPManage> ipManages = null;
		if ("".equals(isBlack)) {
			Pageable pageable = PageRequest.of(Integer.parseInt(pageNumber), Integer.parseInt(pageSize));
			Page<IPManage> ipManage = ipRepo.findAll(pageable);
			return CommonResponse.success(ipManage.get().toList());
		}
		else if ("true".equals(isBlack)) {
			ipManages = ipRepo.findByIsBlackTrue();
		}
		else {
			ipManages = ipRepo.findByIsBlackFalse();
		}
		return CommonResponse.success(ipManages);
	}

	@Override
	public CommonResponse<String> patch(String addresses, String isBlack, String ids) throws ServiceException {
			try {
				String[] addr = addresses.split(",");
				if (StringUtils.isNotEmpty(ids)) {
					ipRepo.deleteById(ids);
					updataCache(isBlack);
					return CommonResponse.success("done");
				}
				for (String ad: addr) {
					InetAddress[] inetAddresses = InetAddress.getAllByName(ad);
					for (InetAddress inetAd: inetAddresses) {
						deleteIPManage(isBlack, inetAd.getHostAddress());
						deleteIPManage(isBlack, inetAd.getHostName());
					}
				}
				updataCache(isBlack);
			} catch (Exception e) {
				throw new ServiceException(e);
			}
		
		return CommonResponse.success("done");
	}

	private void deleteIPManage(String isBlack, String ad) {
		if ("true".equals(isBlack)) {
			ipRepo.deleteByAddressAndIsBlackTrue(ad);
		}
		else {
			ipRepo.deleteByAddressAndIsBlackFalse(ad);
		}
	}

	@Override
	public CommonResponse<List<String>> add(String addrs, String isBlack) throws ServiceException {
		List<String> addresses = null;
		try {
			addresses = new ArrayList<>();
			Boolean flag = false;
			if ("true".equals(isBlack)) {
				flag = true;
			}
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
							addresses.add(ipRepo.save(new IPManage(ad.getHostAddress(), flag)).getAddress());
						}
						if (exist(ad.getHostName(), isBlack) == 0) {
							addresses.add(ipRepo.save(new IPManage(ad.getHostName(), flag)).getAddress());
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

			log.info("black result------------:{}", addresses);
			updataCache(isBlack);
		} catch (Exception e) {
			throw new ServiceException(e);
		}
		return CommonResponse.success(addresses);
	}

	private Long exist(String ad, String isBlack) {
		long size = 0;
		if ("true".equals(isBlack)) {
			size = ipRepo.countByAddressAndIsBlackTrue(ad);
			log.debug("black.size:{}", size);
			return size;
		}
		else {
			size = ipRepo.countByAddressAndIsBlackFalse(ad);
			log.debug("white.size:{}", size);
			return size;
		}
	}

	private void updataCache(String isBlack) {
		StringBuffer cache = new StringBuffer();
		List<IPManage> list = new ArrayList<>();
		KV kv = new KV();
		if ("true".equals(isBlack)) {
			list = ipRepo.findByIsBlackTrue();
			kv.setKey("blackList");
		}
		else {
			list = ipRepo.findByIsBlackFalse();
			kv.setKey("whiteList");
		}
		for (IPManage ad: list) {
			cache.append(ad.getAddress()).append(",");
		}
		kv.setValue(cache.toString().substring(0, cache.length() - 1));
		redisService.set(kv);
	}
//	private void updataCache(String isBlack) {
//		List<String> cache = new ArrayList<>();
//		List<IPManage> list = new ArrayList<>();
//		IPList ipList = new IPList();
//		ipList.setIsBlack(isBlack);
//		if ("true".equals(isBlack)) {
//			list = ipRepo.findByIsBlackTrue();
//		}
//		else {
//			list = ipRepo.findByIsBlackFalse();
//		}
//		for (IPManage ad: list) {
//			cache.add(ad.getAddress());
//		}
//		ipList.setIpList(cache);
//		try {
//			ipListRepo.save(ipList);
//		} catch (Exception e) {
//			log.error("redis excepetion:{}", e);
//		}
//	}

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
			case "black":
				return header.getBlack().split(",");
			case "white":
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
				String list = null;
				flag = false;
				String ip = "";
				if ("true".equals(isBlack)) {
					ip = accessIP(request, "black");
					list = redisService.get("blackList");
				}
				else {
					ip = accessIP(request, "white");
					list = redisService.get("whiteList");
				}
				if (list != null) {
					if (list.contains(ip)) {
						return true;
					} else {
						for (String s : list.split(",")) {
							if (IPRegexUtils.vaildateHost(s)) {
								log.info("query domain: {}", s);
								InetAddress[] inetAddress = InetAddress.getAllByName(s);
								for (InetAddress address : inetAddress) {
									if (address.getHostAddress().equals(ip)) {
										log.info("update domain ip: {}", address.getHostAddress());
										add(address.getHostAddress(), isBlack);
										return true;
									}
								}
							}
						}
					}
				} else {
					updataCache(isBlack);
				}
				/**
				 * 	Todo
				 * 
				 * 	reserve dns need ptr record and public static ip;
				 *  cannot get hostname; need to search first; 
				 *  if exist too many domain record in es may cause problem;		 
				 *
				 *  solved
				 *    query all domain in cache when request don't match cache
				 */
//				InetAddress[] inetAddress = InetAddress.getAllByName(ip);
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
			log.info("flag:{}, black:{}", flag, isBlack);
		} catch (UnknownHostException e) {
			throw new ServiceException(String.valueOf(HttpStatus.SERVICE_UNAVAILABLE.value()), e);
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
			if(IPRegexUtils.vaildateHost(ip)) {
				InetAddress[] inetAddresses = InetAddress.getAllByName(ip);
				for (InetAddress addr: inetAddresses) {
					if (accessIP.equals(addr.getHostAddress())) {
						log.debug("accessIp:{}, ip:{}", accessIP, addr.getHostAddress());
						return true;
					} else {
						log.debug("result accessIp:{}, ip:{}", accessIP, addr.getHostAddress());
					}
				}			
			} else if (accessIP.equals(ip)) {
					log.debug("ip:{}, accessIp:{}", ip, accessIP);
					return true;
			}
		} catch (UnknownHostException e) {
			throw new ServiceException(e);
		}
		return false;
	}
}