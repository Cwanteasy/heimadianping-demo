package com.hmdp.utils;

import cn.hutool.core.io.resource.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID() + "-";
    // private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    // static {
    //     UNLOCK_SCRIPT = new DefaultRedisScript<>();
    //     UNLOCK_SCRIPT.setLocation((Resource) new ClassPathResource("unlock.lua"));
    //     UNLOCK_SCRIPT.setResultType(Long.class);
    // }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean isLocked(Long timeoutSeconds) {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue().
                setIfAbsent(KEY_PREFIX + name, threadId, timeoutSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(success);
    }

    // @Override
    // public void unlock(){
    //     stringRedisTemplate.execute(
    //             UNLOCK_SCRIPT,
    //             Collections.singletonList(KEY_PREFIX + name),
    //             ID_PREFIX + Thread.currentThread().getId());
    // }
    @Override
    public void unlock() {
        // 获取线程标识
        String threadId = ID_PREFIX + Thread.currentThread().getId();
        // 获取锁中标识
        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + name);
        //判断标识是否一致
        if (threadId.equals(id)) {
            //释放锁
            stringRedisTemplate.delete(KEY_PREFIX + name);
        }
    }
}
