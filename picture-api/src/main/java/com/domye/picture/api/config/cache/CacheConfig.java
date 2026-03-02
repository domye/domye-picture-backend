package com.domye.picture.api.config.cache;

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

    /**
     * 用户信息本地缓存
     * 用于缓存用户基础信息
     */
    @Bean
    public Cache<String, String> userInfoLocalCache() {
        return Caffeine.newBuilder()
                .initialCapacity(512)
                .maximumSize(5000L)
                .expireAfterWrite(10L, TimeUnit.MINUTES)
                .build();
    }

    /**
     * 用户关注列表本地缓存
     * 用于缓存用户关注的人 ID 列表
     */
    @Bean
    public Cache<String, String> userFollowsLocalCache() {
        return Caffeine.newBuilder()
                .initialCapacity(256)
                .maximumSize(2000L)
                .expireAfterWrite(5L, TimeUnit.MINUTES)
                .build();
    }
}
