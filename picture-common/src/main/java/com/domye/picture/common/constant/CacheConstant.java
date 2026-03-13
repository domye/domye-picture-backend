package com.domye.picture.common.constant;

/**
 * 缓存相关常量
 */
public interface CacheConstant {
    
    /**
     * 缓存 key 前缀
     */
    String CACHE_PREFIX = "domye:cache:";
    
    /**
     * 分布式锁 key 前缀
     */
    String LOCK_PREFIX = "domye:lock:cache:";
    
    /**
     * 缓存空值标记（防止缓存穿透）
     */
    String NULL_MARKER = "NULL";

    /**
     * 默认缓存随机过期时间范围（防止缓存雪崩）
     */
    long RANDOM_EXPIRE_RANGE = 300L;
    
    /**
     * 分布式锁默认等待时间（秒）
     */
    int DEFAULT_LOCK_WAIT_TIME = 5;

    /**
     * 图片列表缓存 key 前缀
     */
    String PICTURE_LIST_CACHE_KEY = CACHE_PREFIX + "picture:list:";

    /**
     * 用户信息缓存 key 前缀
     */
    String USER_INFO_CACHE_KEY = CACHE_PREFIX + "user:info:";

    /**
     * 用户关注列表缓存 key 前缀
     */
    String USER_FOLLOWS_CACHE_KEY = CACHE_PREFIX + "user:follows:";

}
