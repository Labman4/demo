package com.elpsykongroo.services.redis.client;

import com.elpsykongroo.services.redis.client.dto.KV;

public interface RedisService {
    void set(KV kv);

    String get(String key);

    String getToken(String key);
}
