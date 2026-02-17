package com.domye.picture.api.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 本地缓存配置类
 * 配置 Caffeine 缓存 Bean
 */
@Configuration
public class CacheConfig {

    /**
     * 图片列表查询本地缓存
     * 用于缓存分页查询结果，减少 Redis 和数据库访问
     */
    @Bean
    public Cache<String, String> pictureListLocalCache() {
        return Caffeine.newBuilder()
                .initialCapacity(1024)
                .maximumSize(10000L)
                .expireAfterWrite(5L, TimeUnit.MINUTES)
                .build();
    }
}
