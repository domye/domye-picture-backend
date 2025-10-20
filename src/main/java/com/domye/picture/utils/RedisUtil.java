package com.domye.picture.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 * 提供了Redis的基本操作方法，包括String、Set、Hash、ZSet等数据类型的操作
 * 以及分布式锁的实现
 */
@Component
public class RedisUtil {
    private static StringRedisTemplate stringRedisTemplate;

    /** 设置value **/
    public static void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    public static void setWithExpire(String key, String value, long expireTime, TimeUnit time) {
        stringRedisTemplate.opsForValue().set(key, value, expireTime, time);
    }

    public static String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public static void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    public static Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    public static void expire(String key, long expireTime, TimeUnit time) {
        stringRedisTemplate.expire(key, expireTime, time);
    }

    public static Long getExpire(String key) {
        return stringRedisTemplate.getExpire(key);
    }

    /** 设置set **/
    public static void addSet(String key, String value) {
        stringRedisTemplate.opsForSet().add(key, value);
    }

    public static void deleteSet(String key, String value) {
        stringRedisTemplate.opsForSet().remove(key, value);
    }

    public static Set<String> getSet(String key) {
        return stringRedisTemplate.opsForSet().members(key);
    }

    public static Boolean hasSet(String key, String value) {
        return stringRedisTemplate.opsForSet().isMember(key, value);
    }

    /* 设置hash*/
    public static void setHash(String key, String field, String value) {
        stringRedisTemplate.opsForHash().put(key, field, value);
    }

    public static Map<Object, Object> getHash(String key) {
        return stringRedisTemplate.opsForHash().entries(key);
    }

    public static String getHashValue(String key, String field) {
        return (String) stringRedisTemplate.opsForHash().get(key, field);
    }


    public static void deleteHash(String key, String field) {
        stringRedisTemplate.opsForHash().delete(key, field);
    }

    /*设置zset*/
    public static Double addZSetScore(String key, String value, double score) {
        return stringRedisTemplate.opsForZSet().incrementScore(key, value, score);
    }

    public static Set<ZSetOperations.TypedTuple<String>> getZSetRange(String key, long start, long end) {
        return stringRedisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

    /* 分布式锁*/
    public static boolean tryLock(String lockKey, long expireTime) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(expireTime)));
    }

    public static void unlock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

    public static Long executeLuaScript(String luaScript, List<String> keys, List<String> args) {
        return stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                keys,
                args.toArray()
        );
    }

    @Autowired
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        RedisUtil.stringRedisTemplate = stringRedisTemplate;
    }


}
