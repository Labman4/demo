package com.elpsykongroo.services.redis.server.entity;

import lombok.Data;

@Data
public class KV {
    private String key;

    private String value;

    private String time;
}
