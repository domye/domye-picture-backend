package com.domye.picture.service.cache;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONUtil;
import com.domye.picture.common.constant.CacheConstant;
import com.domye.picture.common.helper.impl.CacheConsistencyHelper;
import com.domye.picture.common.helper.impl.RedisCache;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.RankTimeEnum;
import com.domye.picture.model.mapper.user.UserStructMapper;
import com.domye.picture.model.vo.rank.UserActiveRankItemVO;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.api.user.UserService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 排行榜缓存服务
 * 提供排行榜数据的缓存功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankCacheService {

    private final RedisCache redisCache;
    private final CacheConsistencyHelper cacheConsistencyHelper;
    private final Cache<String, String> activityRankLocalCache;
    private final Cache<String, String> userInfoLocalCache;
    private final UserService userService;
    private final UserStructMapper userStructMapper;

    /**
     * 用户信息缓存过期时间（秒）
     */
    private static final long USER_INFO_EXPIRE_TIME = 1800L;

    private static final String ACTIVITY_SCORE_KEY = "activity_rank_";

    /**
     * 获取排行榜数据的 Redis Key
     *
     * @param rankTimeEnum 排行榜时间类型
     * @return Redis Key
     */
    public String getRankKey(RankTimeEnum rankTimeEnum) {
        Date now = new Date();
        return switch (rankTimeEnum) {
            case DAY -> ACTIVITY_SCORE_KEY + "day:" + DateUtil.format(now, "yyyyMMdd");
            case WEEK -> ACTIVITY_SCORE_KEY + "week:" + DateUtil.format(now, "yyyyWW");
            case MONTH -> ACTIVITY_SCORE_KEY + "month:" + DateUtil.format(now, "yyyyMM");
            case TOTAL -> ACTIVITY_SCORE_KEY + "total";
        };
    }

    /**
     * 获取用户行为去重 Key
     *
     * @param userId 用户ID
     * @return 去重 Key
     */
    public String getUserActionKey(Long userId) {
        return ACTIVITY_SCORE_KEY + "action:" + userId + ":" + DateUtil.format(new Date(), "yyyyMMdd");
    }

    /**
     * 查询排行榜列表（带缓存）
     *
     * @param rankTimeEnum 排行榜时间类型
     * @param size 查询数量
     * @return 排行榜列表
     */
    public List<UserActiveRankItemVO> queryRankList(RankTimeEnum rankTimeEnum, int size) {
        if (size <= 0) {
            return Collections.emptyList();
        }

        String rankKey = getRankKey(rankTimeEnum);

        // 1. 从 Redis 获取排行榜数据
        Set<ZSetOperations.TypedTuple<Object>> userTuples = redisCache
                .reverseRangeWithScores(rankKey, 0, size - 1);

        if (CollUtil.isEmpty(userTuples)) {
            return Collections.emptyList();
        }

        // 2. 提取用户ID
        List<Long> userIds = userTuples.stream()
                .map(tuple -> Long.valueOf((String) Objects.requireNonNull(tuple.getValue())))
                .toList();

        // 3. 批量获取用户信息（带缓存）
        Map<Long, UserVO> userMap = batchGetUserInfo(new HashSet<>(userIds));

        // 4. 构建排行榜项
        List<UserActiveRankItemVO> rankItemList = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<Object> tuple : userTuples) {
            Long userId = Long.valueOf((String) tuple.getValue());
            UserVO userVO = userMap.get(userId);

            if (userVO != null) {
                UserActiveRankItemVO item = new UserActiveRankItemVO();
                item.setUser(userVO);
                item.setScore(tuple.getScore());
                item.setRank(rank++);
                rankItemList.add(item);
            }
        }

        return rankItemList;
    }

    /**
     * 获取用户排名
     *
     * @param userId 用户ID
     * @param rankTimeEnum 排行榜时间类型
     * @return 排名，不存在返回 -1
     */
    public long getUserRank(Long userId, RankTimeEnum rankTimeEnum) {
        if (userId == null) {
            return -1;
        }

        String rankKey = getRankKey(rankTimeEnum);
        Long rank = redisCache.reverseRank(rankKey, String.valueOf(userId));

        return rank != null ? rank + 1 : -1; // ZSet rank is 0-based
    }

    /**
     * 获取用户分数
     *
     * @param userId 用户ID
     * @param rankTimeEnum 排行榜时间类型
     * @return 分数，不存在返回 0
     */
    public double getUserScore(Long userId, RankTimeEnum rankTimeEnum) {
        if (userId == null) {
            return 0;
        }

        String rankKey = getRankKey(rankTimeEnum);
        Double score = redisCache.score(rankKey, String.valueOf(userId));

        return score != null ? score : 0;
    }

    /**
     * 添加用户活跃分数
     *
     * @param userId 用户ID
     * @param score 分数
     * @return 是否成功
     */
    public boolean addActivityScore(Long userId, int score) {
        if (userId == null || score <= 0) {
            return false;
        }

        // 更新所有榜单
        for (RankTimeEnum rankTime : RankTimeEnum.values()) {
            String rankKey = getRankKey(rankTime);
            redisCache.incrementScore(rankKey, String.valueOf(userId), score);

            // 设置过期时间（仅对有时间限制的榜单）
            if (rankTime != RankTimeEnum.TOTAL) {
                Long ttl = redisCache.getExpire(rankKey);
                if (ttl == -1) {
                    redisCache.expireKey(rankKey, 31, TimeUnit.DAYS);
                }
            }
        }

        return true;
    }

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

        // 先从本地缓存获取
        for (Long userId : userIds) {
            String cacheKey = CacheConstant.USER_INFO_CACHE_KEY + userId;
            String cachedValue = userInfoLocalCache.getIfPresent(cacheKey);

            if (cachedValue != null) {
                if (!CacheConstant.NULL_MARKER.equals(cachedValue)) {
                    try {
                        UserVO userVO = JSONUtil.toBean(cachedValue, UserVO.class);
                        result.put(userId, userVO);
                    } catch (Exception e) {
                        log.warn("解析用户缓存失败: {}", cachedValue, e);
                        missedIds.add(userId);
                    }
                }
            } else {
                // 尝试从 Redis 获取
                Object redisValue = redisCache.get(cacheKey);
                if (redisValue != null) {
                    if (!CacheConstant.NULL_MARKER.equals(redisValue)) {
                        try {
                            UserVO userVO = JSONUtil.toBean(String.valueOf(redisValue), UserVO.class);
                            result.put(userId, userVO);
                            userInfoLocalCache.put(cacheKey, String.valueOf(redisValue));
                        } catch (Exception e) {
                            log.warn("解析 Redis 用户缓存失败: {}", redisValue, e);
                            missedIds.add(userId);
                        }
                    } else {
                        userInfoLocalCache.put(cacheKey, CacheConstant.NULL_MARKER);
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
                    redisCache.put(cacheKey, CacheConstant.NULL_MARKER, USER_INFO_EXPIRE_TIME);
                    userInfoLocalCache.put(cacheKey, CacheConstant.NULL_MARKER);
                }
            }
        }

        return result;
    }

}