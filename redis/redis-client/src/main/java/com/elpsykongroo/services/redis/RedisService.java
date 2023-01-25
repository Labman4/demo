package com.elpsykongroo.services.redis;

import com.elpsykongroo.services.redis.dto.KV;

public interface RedisService {
    void set(KV kv);

    String get(String key);
}
