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

import com.elpsykongroo.auth.service.custom.GroupService;
import com.elpsykongroo.base.common.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/group")
@Slf4j
public class GroupController {

    @Autowired
    private GroupService groupService;

    @GetMapping
    public String groupList() {
        return CommonResponse.data(groupService.groupList());
    }

    @PutMapping
    public String addGroup(@RequestParam String name) {
        return CommonResponse.string(groupService.addGroup(name));
    }

    @DeleteMapping("/{name}")
    public String deleteGroup(@PathVariable String name) {
       return CommonResponse.data(groupService.deleteGroup(name));
    }

    @GetMapping("/user/{id}")
    public String userGroupList(@PathVariable String id) {
        return CommonResponse.data(groupService.userGroup(id));
    }

    @GetMapping("/authority/{name}")
    public String groupAuthorityList(@PathVariable String name) {
        return CommonResponse.data(groupService.findByAuthority(name));
    }

    @PostMapping("/user")
    public String updateGroup(@RequestParam String groups,
                              @RequestParam String ids) {
        return CommonResponse.data(groupService.updateUserGroup(groups, ids));
    }
}
