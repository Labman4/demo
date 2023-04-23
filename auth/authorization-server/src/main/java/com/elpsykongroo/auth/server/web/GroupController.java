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

package com.elpsykongroo.auth.server.web;

import com.elpsykongroo.auth.server.service.custom.GroupService;
import com.elpsykongroo.base.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @GetMapping("/list")
    public String groupList(
    ) {
        return JsonUtils.toJson(groupService.groupList());
    }

    @PutMapping("/add")
    public String addGroup(
            @RequestParam String group
    ) {
        try {
            groupService.addGroup(group);
            return "done";
        } catch (Exception e) {
            log.error("add group error:{}", e.getMessage());
            return "0";
        }
    }

    @DeleteMapping("/delete/{name}")
    public String deleteGroup(
            @PathVariable String name
    ) {
        if (groupService.deleteGroup(name) > 0) {
            return "done";
        }
        return "0";
    }

    @GetMapping("/user/list")
    public String userGroupList(
            @RequestParam String id
    ) {
        return JsonUtils.toJson(groupService.userGroup(id));
    }

    @GetMapping("/authority/list")
    public String groupAuthorityList(
            @RequestParam String name
    ) {
        return JsonUtils.toJson(groupService.findByAuthority(name));
    }

    @PatchMapping("/user/patch")
    public String updateGroup(
            @RequestParam("groups") String groups,
            @RequestParam("ids") String ids
    ) {
        if (groupService.updateUserGroup(groups, ids) > 0) {
            return "done";
        }
        return "0";
    }


}
