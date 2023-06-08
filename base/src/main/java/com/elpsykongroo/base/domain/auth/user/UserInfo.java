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

package com.elpsykongroo.base.domain.auth.user;

import lombok.Data;

@Data
public class UserInfo {
    public String sub;
    public String name;
    public String given_name;
    public String family_name;
    public String middle_name;
    public String nickname;
    public String preferred_username;
    public String profile;
    public String picture;
    public String website;
    public String email;
    public String email_verified;
    public String gender;
    public String birthdate;
    public String zoneinfo;
    public String locale;
    public String phone_number;
    public String phone_number_verified;
    public String address;
    public String updated_at;
    public String claims;
    public String username;

}
