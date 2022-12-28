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

import jakarta.servlet.http.HttpServletRequest;

import com.elpsykongroo.demo.common.CommonResponse;
import com.elpsykongroo.demo.config.RequestConfig;
import com.elpsykongroo.demo.config.RequestConfig.Header;
import com.elpsykongroo.demo.document.IPManage;
import com.elpsykongroo.demo.exception.ElasticException;
import com.elpsykongroo.demo.repo.IPRepo;
import com.elpsykongroo.demo.service.IPManagerService;
import com.elpsykongroo.demo.utils.PathUtils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class IPMangerServiceImpl implements IPManagerService {
	private IPRepo ipRepo;

	public IPMangerServiceImpl(IPRepo ipRepo) {
		this.ipRepo = ipRepo;
	}
//    @Autowired
//    private RedissonClient redissonClient;

	@Autowired
	private RedisTemplate redisTemplate;

    @Autowired
	private RequestConfig requestConfig;

//	@Autowired
//	private DemoMapper demoMapper;

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
	public CommonResponse<String> patch(String addresses, String isBlack, String ids) throws UnknownHostException {
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
	public CommonResponse<List<String>> add(String addrs, String isBlack) {
		List<String> addresses = new ArrayList<>();
		Boolean flag = false;
		if ("true".equals(isBlack)) {
			flag = true;
		}
		try {
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
		}
		catch (UnknownHostException e) {
			return CommonResponse.error(HttpStatus.BAD_REQUEST.value(), "unknown host");
		}
		catch (ElasticException e) {
			return CommonResponse.error(HttpStatus.SERVICE_UNAVAILABLE.value(), "service timeout");
		}
		log.info("black result------------:{}", addresses);
		updataCache(isBlack);
		return CommonResponse.success(addresses);
	}

	private Long exist(String ad, String isBlack) {
		long size = 0;
		if ("true".equals(isBlack)) {
			size = ipRepo.countByAddressAndIsBlackTrue(ad);
			log.info("black.size:{}", size);
			return size;
		}
		else {
			size = ipRepo.countByAddressAndIsBlackFalse(ad);
			log.info("white.size:{}", size);
			return size;
		}
	}

	private void updataCache(String isBlack) {
		List<String> cache = new ArrayList<>();
		List<IPManage> list = new ArrayList<>();
		if ("true".equals(isBlack)) {
			list = ipRepo.findByIsBlackTrue();
		}
		else {
			list = ipRepo.findByIsBlackFalse();
		}
		for (IPManage ad: list) {
			cache.add(ad.getAddress());
		}
		if ("true".equals(isBlack)) {
			redisTemplate.opsForValue().set("blacklist", cache.toString());
		}
		else {
			redisTemplate.opsForValue().set("whitelist", cache.toString());
		}
	}

	@Override
	public String accessIP(HttpServletRequest request, String headerType) {
		Header header = requestConfig.getHeader();
		String[] headers = header.getIp().split(",");
		
		if ("black".equals(headerType)) {
			headers = header.getBlack().split(",");
		}
		if ("white".equals(headerType)) {
			headers = header.getWhite().split(",");
		}
		String ip = null;
		for (String head: headers) {
			ip = request.getHeader(head);
			if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
				continue;
			}
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		if (!PathUtils.beginWithPath(requestConfig.getPath().getExclude().getRecord(), request.getRequestURI())) {
			log.info("ip------------{}", ip);
		}
		return ip;
	}

	public Boolean blackOrWhiteList(HttpServletRequest request, String isBlack) throws UnknownHostException {
		Object list = null;
		boolean flag = false;
		String ip = "";
		if ("true".equals(isBlack)) {
			ip = accessIP(request, "black");
			list = redisTemplate.opsForValue().get("blacklist");
		}
		else {
			ip = accessIP(request, "white");
			list = redisTemplate.opsForValue().get("whitelist");
		}
		if (list != null) {
			if (list.toString().contains(ip)) {
				flag = true;
			}
		}
		InetAddress[] inetAddress = InetAddress.getAllByName(ip);
		if (exist(ip, isBlack) > 0) {
			flag = true;
			for (InetAddress address: inetAddress) {
				if (!ip.equals(address.getHostName()) && exist(address.getHostName(), isBlack) == 0) {
					String newAddress = ipRepo.save(new IPManage(address.getHostName(), Boolean.valueOf(isBlack)))
							.getAddress();
					updataCache(isBlack);
					log.info("Update blacklist domain when blackIP domain change, {} -> {}", ip, newAddress);
				}
			}
		}
		else {
			for (InetAddress address: inetAddress) {
				if (exist(address.getHostName(), isBlack) > 0) {
					log.info("hostname in list");
					flag = true;
				}
			}
		}
		log.info("flag:{}, black:{}", flag, isBlack);
		return flag;
	}
}