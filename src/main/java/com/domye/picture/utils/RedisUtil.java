package com.domye.picture.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    private static StringRedisTemplate stringRedisTemplate;

    public static void set(String key, String value, long expireTime, TimeUnit time) {
        stringRedisTemplate.opsForValue().set(key, value, expireTime, time);
    }

    public static String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public static void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        RedisUtil.stringRedisTemplate = stringRedisTemplate;
    }
}
