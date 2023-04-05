package com.elpsykongroo.auth.server.entity.user;

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
