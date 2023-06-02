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

package com.elpsykongroo.services.elasticsearch.server.service.impl;

import com.elpsykongroo.services.elasticsearch.server.domain.IPManage;
import com.elpsykongroo.services.elasticsearch.server.repo.IPRepo;
import com.elpsykongroo.services.elasticsearch.server.service.IPManageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IPManageServiceImpl implements IPManageService {
    private IPRepo ipRepo;

    @Autowired
    public IPManageServiceImpl(IPRepo ipRepo) {
        this.ipRepo = ipRepo;
    }

    @Override
    public IPManage save(String address, String black) {
        IPManage ipManage = new IPManage(address, Boolean.valueOf(black));
        return ipRepo.save(ipManage);
    }

    @Override
    public String ipList(String black) {
        if ("true".equals(black)) {
            return ipRepo.findByIsBlackTrue();
        } else {
            return ipRepo.findByIsBlackFalse();
        }
    }

    @Override
    public List<IPManage> ipPageList(Pageable pageable, String black) {
        if (StringUtils.isBlank(black)) {
            return ipRepo.findAll(pageable).get().toList();
        } else if ("true".equals(black)) {
            return ipRepo.findByIsBlackTrue(pageable);
        } else {
            return ipRepo.findByIsBlackFalse(pageable);
        }
    }

    @Override
    public String count(String address, String black) {
        if ("true".equals(black)) {
            return ipRepo.countByAddressAndIsBlackTrue(address);
        } else {
            return ipRepo.countByAddressAndIsBlackFalse(address);
        }
    }

    @Override
    public String deleteByAddressAndIsBlackTrue(String address) {
       return ipRepo.deleteByAddressAndIsBlackTrue(address);
    }

    @Override
    public String deleteByAddressAndIsBlackFalse(String address) {
       return ipRepo.deleteByAddressAndIsBlackFalse(address);
    }

    @Override
    public void deleteById(String id) {
        ipRepo.deleteById(id);
    }
}
