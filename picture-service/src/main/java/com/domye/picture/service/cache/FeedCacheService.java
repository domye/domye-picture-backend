package com.domye.picture.service.cache;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.domye.picture.common.constant.CacheConstant;
import com.domye.picture.common.helper.impl.CacheConsistencyHelper;
import com.domye.picture.common.helper.impl.RedisCache;
import com.domye.picture.model.entity.contact.Contact;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.mapper.user.UserStructMapper;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.api.contact.ContactService;
import com.domye.picture.service.api.user.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 信息流缓存服务
 * 提供用户信息、关注列表的缓存功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedCacheService {

    private final RedisCache redisCache;
    private final CacheConsistencyHelper cacheConsistencyHelper;
    private final Cache<String, String> userInfoLocalCache;
    private final Cache<String, String> userFollowsLocalCache;
    private final UserService userService;
    private final ContactService contactService;
    private final UserStructMapper userStructMapper;

    /**
     * 用户信息缓存过期时间（秒）
     */
    private static final long USER_INFO_EXPIRE_TIME = 1800L;

    /**
     * 关注列表缓存过期时间（秒）
     */
    private static final long USER_FOLLOWS_EXPIRE_TIME = 600L;

    /**
     * 批量获取用户信息（带缓存）
     *
     * @param userIds 用户ID集合
     * @return 用户ID -> 用户信息的映射
     */
    public Map<Long, UserVO> batchGetUserInfo(Set<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }

        Map<Long, UserVO> result = new HashMap<>();
        List<Long> missedIds = new ArrayList<>();

        // 先从缓存获取
        for (Long userId : userIds) {
            String cacheKey = CacheConstant.USER_INFO_CACHE_KEY + userId;
            String cachedValue = userInfoLocalCache.getIfPresent(cacheKey);

            if (cachedValue != null) {
                if (!"NULL".equals(cachedValue)) {
                    try {
                        UserVO userVO = JSONUtil.toBean(cachedValue, UserVO.class);
                        result.put(userId, userVO);
                    } catch (Exception e) {
                        log.warn("解析用户缓存失败: {}", cachedValue, e);
                        missedIds.add(userId);
                    }
                }
                // NULL 标记表示用户不存在，跳过
            } else {
                // 尝试从 Redis 获取
                Object redisValue = redisCache.get(cacheKey);
                if (redisValue != null) {
                    if (!"NULL".equals(redisValue)) {
                        try {
                            UserVO userVO = JSONUtil.toBean(String.valueOf(redisValue), UserVO.class);
                            result.put(userId, userVO);
                            userInfoLocalCache.put(cacheKey, String.valueOf(redisValue));
                        } catch (Exception e) {
                            log.warn("解析 Redis 用户缓存失败: {}", redisValue, e);
                            missedIds.add(userId);
                        }
                    } else {
                        userInfoLocalCache.put(cacheKey, "NULL");
                    }
                } else {
                    missedIds.add(userId);
                }
            }
        }

        // 批量查询未命中缓存的用户
        if (!missedIds.isEmpty()) {
            List<User> users = userService.listByIds(missedIds);
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            for (Long userId : missedIds) {
                String cacheKey = CacheConstant.USER_INFO_CACHE_KEY + userId;
                User user = userMap.get(userId);

                if (user != null) {
                    UserVO userVO = userStructMapper.toUserVo(user);
                    String jsonValue = JSONUtil.toJsonStr(userVO);
                    redisCache.put(cacheKey, jsonValue, USER_INFO_EXPIRE_TIME);
                    userInfoLocalCache.put(cacheKey, jsonValue);
                    result.put(userId, userVO);
                } else {
                    // 缓存空值防止穿透
                    redisCache.put(cacheKey, "NULL", USER_INFO_EXPIRE_TIME);
                    userInfoLocalCache.put(cacheKey, "NULL");
                }
            }
        }

        return result;
    }

    /**
     * 获取用户关注列表（带缓存）
     *
     * @param userId 用户ID
     * @return 关注的用户ID列表
     */
    public List<Long> getUserFollows(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }

        String cacheKey = CacheConstant.USER_FOLLOWS_CACHE_KEY + userId;

        return cacheConsistencyHelper.getWithBreakdownProtection(
                userFollowsLocalCache,
                cacheKey,
                USER_FOLLOWS_EXPIRE_TIME,
                () -> {
                    List<Contact> contacts = contactService.lambdaQuery()
                            .eq(Contact::getUserId, userId)
                            .eq(Contact::getStatus, 1) // 已接受的关注关系
                            .list();

                    return contacts.stream()
                            .map(Contact::getContactUserId)
                            .collect(Collectors.toList());
                }
        );
    }

}