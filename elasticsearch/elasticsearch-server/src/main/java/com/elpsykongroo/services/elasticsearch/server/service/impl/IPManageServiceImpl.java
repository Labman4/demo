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
    public IPManage save(IPManage ipManage) {
        return ipRepo.save(ipManage);
    }

    @Override
    public List<IPManage> findAll(Pageable pageable) {
        return ipRepo.findAll(pageable).get().toList();
    }

    @Override
    public List<IPManage> findByIsBlackTrue() {
        return ipRepo.findByIsBlackTrue();
    }

    @Override
    public List<IPManage> findByIsBlackFalse() {
        return ipRepo.findByIsBlackFalse();
    }

    @Override
    public List<IPManage> findByIsBlackTrue(Pageable pageable) {
        return ipRepo.findByIsBlackTrue(pageable);
    }

    @Override
    public List<IPManage> findByIsBlackFalse(Pageable pageable) {
        return ipRepo.findByIsBlackFalse(pageable);
    }

    @Override
    public long countByAddressAndIsBlackTrue(String address) {
        return ipRepo.countByAddressAndIsBlackTrue(address);
    }

    @Override
    public long countByAddressAndIsBlackFalse(String address) {
        return ipRepo.countByAddressAndIsBlackFalse(address);
    }

    @Override
    public int deleteByAddressAndIsBlackTrue(String address) {
       return ipRepo.deleteByAddressAndIsBlackTrue(address);
    }

    @Override
    public int deleteByAddressAndIsBlackFalse(String address) {
       return ipRepo.deleteByAddressAndIsBlackFalse(address);
    }

    @Override
    public void deleteById(String id) {
        ipRepo.deleteById(id);
    }
}
