package com.elpsykongroo.demo.services.redis;

import com.elpsykongroo.services.redis.RedisService;
import com.elpsykongroo.services.redis.dto.KV;
import com.elpsykongroo.services.redis.impl.RedisServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

//@RestClientTest(RedisService.class)
public class RedisServiceTest {

    private RestTemplate restTemplate = new RestTemplate();

    private RedisService redisService = new RedisServiceImpl("http://localhost:8379", restTemplate);

    private MockRestServiceServer server =  MockRestServiceServer.bindTo(restTemplate).build();

    @Test
    void set() {
        this.server.expect(requestTo("http://localhost:8379/redis/set")).andRespond(withSuccess());
        this.redisService.set(new KV());
    }
    @Test
    void get() {
        this.server.expect(requestTo("http://localhost:8379/redis/get")).andRespond(withSuccess());
        this.redisService.get(new KV());
    }
}
