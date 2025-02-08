package com.hmdp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class RedissonTest {
    @Resource
    private RedissonClient redissonClient;

    private RLock lock;

    @BeforeEach
    public void setUp() throws Exception {
        lock = redissonClient.getLock("order");
    }

    @Test
    public void testRedisson() throws InterruptedException {
        boolean isLock = lock.tryLock(1L, TimeUnit.MINUTES);
    }

}
