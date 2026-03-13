package com.domye.picture.common.helper.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.domye.picture.common.constant.CacheConstant;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.domye.picture.common.constant.CacheConstant.NULL_MARKER;

/**
 * 缓存一致性辅助类
 * 提供缓存双写一致性、防击穿、防穿透、缓存预热等功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheConsistencyHelper {

    private final RedisCache redisCache;
    private final LockService lockService;

    /**
     * 获取缓存（带防击穿逻辑）
     * 使用分布式锁 + 双重检查机制防止缓存击穿
     *
     * @param localCache    本地缓存
     * @param key           缓存键
     * @param expireTime    Redis 缓存过期时间（秒）
     * @param loader        数据加载器（当缓存不存在时从数据库加载）
     * @param <T>           返回值类型
     * @return 缓存值或从数据库加载的值
     */
    public <T> T getWithBreakdownProtection(Cache<String, String> localCache,
                                            String key,
                                            Long expireTime,
                                            Supplier<T> loader) {
        // 1. 先查本地缓存
        String localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            if (NULL_MARKER.equals(localValue)) {
                return null;
            }
            return parseValue(localValue);
        }

        // 2. 查 Redis 缓存
        String redisValue = (String) redisCache.get(key);
        if (redisValue != null) {
            if (NULL_MARKER.equals(redisValue)) {
                localCache.put(key, NULL_MARKER);
                return null;
            }
            localCache.put(key, redisValue);
            return parseValue(redisValue);
        }

        // 3. 使用分布式锁防止击穿（双重检查）
        String lockKey = CacheConstant.LOCK_PREFIX + key;
        return lockService.executeWithLock(lockKey, CacheConstant.DEFAULT_LOCK_WAIT_TIME, TimeUnit.SECONDS, () -> {
            // 双重检查：获取锁后再次检查缓存
            String doubleCheckValue = (String) redisCache.get(key);
            if (doubleCheckValue != null) {
                if (NULL_MARKER.equals(doubleCheckValue)) {
                    localCache.put(key, NULL_MARKER);
                    return null;
                }
                localCache.put(key, doubleCheckValue);
                return parseValue(doubleCheckValue);
            }

            // 4. 从数据库加载数据
            T data = loader.get();
            String cacheValue = data != null ? serializeValue(data) : NULL_MARKER;
            
            // 5. 写入缓存（随机过期时间防止雪崩）
            long actualExpireTime = expireTime + RandomUtil.randomLong(0, CacheConstant.RANDOM_EXPIRE_RANGE);
            redisCache.put(key, cacheValue, actualExpireTime);
            
            // 6. 写入本地缓存
            localCache.put(key, cacheValue);
            
            log.debug("Cache loaded for key: {}, expireTime: {}s", key, actualExpireTime);
            return data;
        });
    }

    /**
     * 序列化缓存值（使用 JSON 序列化）
     */
    private <T> String serializeValue(T value) {
        if (value == null) {
            return NULL_MARKER;
        }
        return JSONUtil.toJsonStr(value);
    }

    /**
     * 解析缓存值（简单实现，返回原始字符串）
     * 适用于简单类型或调用方自行处理反序列化的场景
     */
    @SuppressWarnings("unchecked")
    private <T> T parseValue(String value) {
        if (value == null || NULL_MARKER.equals(value)) {
            return null;
        }
        // 返回原始字符串，调用方需要自行处理类型转换
        return (T) value;
    }

}
