package com.domye.picture.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.model.rank.dto.UserActivityScoreAddRequest;
import com.domye.picture.model.rank.enums.RankTimeEnum;
import com.domye.picture.model.rank.vo.UserActiveRankItemVO;
import com.domye.picture.model.user.entity.User;
import com.domye.picture.service.RankService;
import com.domye.picture.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RankServiceImpl implements RankService {
    private static final String ACTIVITY_SCORE_KEY = "activity_rank_";
    Date today = new Date();
    @Resource
    private UserService userService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 当天活跃度排行榜
     * @return 当天排行榜key
     */
    private String todayRankKey() {
        return ACTIVITY_SCORE_KEY + DateUtil.format(today, "yyyyMMdd");
    }

    /*
      本月排行榜
      @return 月度排行榜key
     */
    private String monthRankKey() {
        return ACTIVITY_SCORE_KEY + DateUtil.format(today, "yyyyMM");
    }

    @Override
    public Boolean addActivityScore(User user, UserActivityScoreAddRequest userActivityScoreAddRequest) {
        //检查参数
        Throw.throwIf(user == null, ErrorCode.PARAMS_ERROR);
        long userId = user.getId();
        Throw.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR);
        String field;
        int score = 0;
        if (userActivityScoreAddRequest.getPath() != null) {
            field = "path_" + userActivityScoreAddRequest.getPath();
            score = 1;
        } else if (userActivityScoreAddRequest.getPictureId() != null) {
            field = userActivityScoreAddRequest.getPictureId() + "_";
            if (BooleanUtils.isTrue(userActivityScoreAddRequest.getUploadPicture())) {
                // 发布文章
                field += "publish";
                score += 10;
            }
        } else {
            return true;
        }
        final String todayRankKey = todayRankKey();
        final String monthRankKey = monthRankKey();
        final String userActionKey = ACTIVITY_SCORE_KEY + user.getId() + DateUtil.format(new Date(), "yyyyMMdd");
        String ansStr = (String) stringRedisTemplate.opsForHash().get(userActionKey, field);
        Integer ans = ansStr != null ? Integer.parseInt(ansStr) : null;
        //如果不存在，执行加分
        if (ans == null) {
            stringRedisTemplate.opsForHash().put(userActionKey, field, String.valueOf(score));
            stringRedisTemplate.expire(userActionKey, 31, TimeUnit.DAYS);
            Double newAns = stringRedisTemplate.opsForZSet().incrementScore(todayRankKey, String.valueOf(userId), score);
            stringRedisTemplate.opsForZSet().incrementScore(monthRankKey, String.valueOf(userId), score);
            if (log.isDebugEnabled()) {
                log.info("活跃度更新加分! key#field = {}#{}, add = {}, newScore = {}", todayRankKey, userId, score, newAns);
            }
            if (newAns <= score) {
                Long ttl = stringRedisTemplate.getExpire(todayRankKey);
                if (ttl == -1) {
                    stringRedisTemplate.expire(todayRankKey, 31, TimeUnit.DAYS);
                }
                ttl = stringRedisTemplate.getExpire(monthRankKey);
                if (ttl == -1) {
                    stringRedisTemplate.expire(monthRankKey, 31, TimeUnit.DAYS);
                }
            }
        }
        return true;
    }

    @Override
    public List<UserActiveRankItemVO> queryRankList(int value, int size) {
        String rankKey = RankTimeEnum.isDay(value) ? todayRankKey() : monthRankKey();

        // 1. 获取topN的活跃用户
        // 使用Redis的ZSet获取分数最高的用户ID列表
        Set<String> userIdSet = stringRedisTemplate.opsForZSet().reverseRange(rankKey, 0, size - 1);
        if (CollUtil.isEmpty(userIdSet)) {
            return Collections.emptyList();
        }

        // 2. 查询用户的基本信息
        // 将用户ID字符串转换为Long类型
        List<Long> userIds = userIdSet.stream()
                .map(Long::valueOf)
                .collect(Collectors.toList());

        // 批量查询用户信息
        List<User> userList = userService.listByIds(userIds);
        if (CollUtil.isEmpty(userList)) {
            return Collections.emptyList();
        }

        // 创建用户ID到用户的映射
        Map<Long, User> userMap = userList.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 3. 根据评分进行排序
        // 获取用户ID对应的分数
        List<UserActiveRankItemVO> rankItemList = new ArrayList<>();
        for (String userIdStr : userIdSet) {
            Long userId = Long.valueOf(userIdStr);
            User user = userMap.get(userId);
            if (user != null) {
                // 获取用户在排行榜中的分数
                Double score = stringRedisTemplate.opsForZSet().score(rankKey, userIdStr);
                UserActiveRankItemVO item = new UserActiveRankItemVO();
                // 设置用户信息
                item.setUser(userService.getUserVO(user));
                // 设置分数
                item.setScore(score);
                rankItemList.add(item);
            }
        }

        // 4. 补齐每个用户的排名
        for (int i = 0; i < rankItemList.size(); i++) {
            rankItemList.get(i).setRank(i + 1);
        }

        return rankItemList;
    }

}
