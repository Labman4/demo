package com.elpsykongroo.services.redis.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class KV {
    private String key;

    public KV(String key, String value) {
        this.key = key;
        this.value = value;
    }

    private String value;

}
