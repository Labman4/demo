package com.elpsykongroo.services.redis.entity;

import lombok.Data;

@Data
public class KV {
    private String key;

    private String value;
}
