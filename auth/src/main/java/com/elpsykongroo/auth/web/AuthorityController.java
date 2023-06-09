/*
 * Copyright 2020-2022 the original author or authors.
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

package com.elpsykongroo.auth.web;

import com.elpsykongroo.auth.service.custom.AuthorityService;
import com.elpsykongroo.base.common.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth/authority")
@Slf4j
public class AuthorityController {
    @Autowired
    private AuthorityService authorityService;

    @PatchMapping("/group")
    public String updateGroupAuthority(@RequestParam String authorities,
                                       @RequestParam String ids) {
        return CommonResponse.data(authorityService.updateGroupAuthority(authorities, ids));
    }

    @PatchMapping("/user")
    public String updateUserAuthority(@RequestParam String authorities,
                                      @RequestParam String ids) {
        return CommonResponse.data(authorityService.updateUserAuthority(authorities, ids));
    }

    @GetMapping("/user/{id}")
    public String userAuthorityList(@PathVariable String id) {
        return CommonResponse.data(authorityService.userAuthority(id));
    }

    @DeleteMapping("/delete/{name}")
    public String deleteAuthority(@PathVariable String name) {
        return CommonResponse.data(authorityService.deleteAuthority(name));
    }

    @GetMapping
    public String authorityList(
    ) {
        return CommonResponse.data(authorityService.authorityList());
    }

    @GetMapping("/group/{name}")
    public String authorityGroupList(@PathVariable String name) {
        return CommonResponse.data(authorityService.findByGroup(name));
    }

    @PutMapping("/{name}")
    public String addAuthority(
            @PathVariable String name
    ) {
        return  CommonResponse.string(authorityService.addAuthority(name));
    }
}
