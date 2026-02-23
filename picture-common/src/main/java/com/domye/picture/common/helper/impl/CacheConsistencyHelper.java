package com.domye.picture.common.helper.impl;

import cn.hutool.core.util.RandomUtil;
import com.domye.picture.common.constant.CacheConstant;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
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
     * 获取缓存（带防击穿逻辑，使用默认过期时间）
     */
    public <T> T getWithBreakdownProtection(Cache<String, String> localCache,
                                            String key,
                                            Supplier<T> loader) {
        return getWithBreakdownProtection(localCache, key, CacheConstant.DEFAULT_EXPIRE_TIME, loader);
    }

    /**
     * 缓存双写一致性 - 先更新数据库，再删除缓存（Cache-Aside Pattern）
     * 这种策略可以最大程度保证一致性，适合读多写少的场景
     *
     * @param localCache    本地缓存
     * @param key           缓存键
     * @param dbOperation   数据库操作
     * @param <T>           返回值类型
     * @return 数据库操作结果
     */
    public <T> T updateWithCacheInvalidation(Cache<String, String> localCache,
                                             String key,
                                             Supplier<T> dbOperation) {
        // 执行数据库操作
        T result = dbOperation.get();
        
        // 删除缓存（先更新数据库，再删除缓存）
        invalidateCache(localCache, key);
        
        return result;
    }

    /**
     * 缓存双写一致性 - 先删除缓存，再更新数据库
     * 适合对一致性要求较低，但对性能要求较高的场景
     *
     * @param localCache    本地缓存
     * @param key           缓存键
     * @param dbOperation   数据库操作
     * @param <T>           返回值类型
     * @return 数据库操作结果
     */
    public <T> T updateWithCachePreInvalidation(Cache<String, String> localCache,
                                                String key,
                                                Supplier<T> dbOperation) {
        // 先删除缓存
        invalidateCache(localCache, key);
        
        // 执行数据库操作
        return dbOperation.get();
    }

    /**
     * 延迟双删策略
     * 适用于高并发场景，通过延迟二次删除解决脏数据问题
     *
     * @param localCache    本地缓存
     * @param key           缓存键
     * @param dbOperation   数据库操作
     * @param delayMillis   延迟删除时间（毫秒）
     * @param <T>           返回值类型
     * @return 数据库操作结果
     */
    public <T> T updateWithDelayedDoubleInvalidation(Cache<String, String> localCache,
                                                     String key,
                                                     Supplier<T> dbOperation,
                                                     long delayMillis) {
        // 第一次删除缓存
        invalidateCache(localCache, key);
        
        // 执行数据库操作
        T result = dbOperation.get();
        
        // 延迟后第二次删除缓存
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delayMillis);
                invalidateCache(localCache, key);
                log.debug("Delayed cache invalidation completed for key: {}", key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Delayed cache invalidation interrupted for key: {}", key);
            }
        });
        
        return result;
    }

    /**
     * 缓存预热
     * 在系统启动时或低峰期预加载热点数据
     *
     * @param localCache 本地缓存
     * @param key        缓存键
     * @param loader     数据加载器
     * @param expireTime 过期时间（秒）
     * @param <T>        数据类型
     */
    public <T> void warmUpCache(Cache<String, String> localCache,
                                String key,
                                Supplier<T> loader,
                                Long expireTime) {
        try {
            T data = loader.get();
            if (data != null) {
                String cacheValue = serializeValue(data);
                long actualExpireTime = expireTime + RandomUtil.randomLong(0, CacheConstant.RANDOM_EXPIRE_RANGE);
                redisCache.put(key, cacheValue, actualExpireTime);
                localCache.put(key, cacheValue);
                log.info("Cache warmed up for key: {}", key);
            }
        } catch (Exception e) {
            log.error("Failed to warm up cache for key: {}", key, e);
        }
    }

    /**
     * 批量缓存预热
     *
     * @param localCache 本地缓存
     * @param keyLoader  缓存键和数据加载器的映射
     * @param expireTime 过期时间（秒）
     * @param <T>        数据类型
     */
    public <T> void warmUpCacheBatch(Cache<String, String> localCache,
                                     java.util.Map<String, Supplier<T>> keyLoader,
                                     Long expireTime) {
        keyLoader.forEach((key, loader) -> warmUpCache(localCache, key, loader, expireTime));
    }

    /**
     * 使缓存失效（删除本地缓存和 Redis 缓存）
     *
     * @param localCache 本地缓存
     * @param key        缓存键
     */
    public void invalidateCache(Cache<String, String> localCache, String key) {
        // 删除本地缓存
        localCache.invalidate(key);
        
        // 删除 Redis 缓存
        redisCache.remove(key);
        
        log.debug("Cache invalidated for key: {}", key);
    }

    /**
     * 批量使缓存失效
     *
     * @param localCache 本地缓存
     * @param keys       缓存键列表
     */
    public void invalidateCacheBatch(Cache<String, String> localCache, java.util.List<String> keys) {
        keys.forEach(key -> invalidateCache(localCache, key));
    }

    /**
     * 模糊匹配使缓存失效
     *
     * @param localCache 本地缓存
     * @param keyPattern 缓存键模式（支持 * 通配符）
     */
    public void invalidateCacheByPattern(Cache<String, String> localCache, String keyPattern) {
        // 删除本地缓存中匹配的键
        localCache.asMap().keySet().stream()
                .filter(key -> matchPattern(key, keyPattern))
                .forEach(localCache::invalidate);
        
        // 删除 Redis 中匹配的键
        redisCache.vagueDel(keyPattern.replace("*", ""));
        
        log.debug("Cache invalidated by pattern: {}", keyPattern);
    }

    /**
     * 简单的模式匹配
     */
    private boolean matchPattern(String key, String pattern) {
        if (pattern.endsWith("*")) {
            return key.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return key.equals(pattern);
    }

    /**
     * 序列化缓存值（简单实现，实际项目中可使用 JSON 序列化）
     */
    private <T> String serializeValue(T value) {
        if (value == null) {
            return NULL_MARKER;
        }
        // 使用 toString 作为简单序列化，实际项目应使用 JSON
        return value.toString();
    }

    /**
     * 解析缓存值（简单实现）
     */
    @SuppressWarnings("unchecked")
    private <T> T parseValue(String value) {
        if (value == null || NULL_MARKER.equals(value)) {
            return null;
        }
        // 这里返回原始字符串，实际使用时需要配合具体的类型转换
        return (T) value;
    }
}
