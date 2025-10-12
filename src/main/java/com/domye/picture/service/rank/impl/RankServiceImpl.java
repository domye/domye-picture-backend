package com.domye.picture.service.rank.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.service.picture.PictureService;
import com.domye.picture.service.picture.model.entity.Picture;
import com.domye.picture.service.rank.RankService;
import com.domye.picture.service.rank.model.dto.UserActivityScoreAddRequest;
import com.domye.picture.service.rank.model.enums.RankTimeEnum;
import com.domye.picture.service.rank.model.vo.UserActiveRankItemVO;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
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
    @Autowired
    private PictureService pictureService;

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
            Picture picture = pictureService.getById(userActivityScoreAddRequest.getPath());
            if (picture == null)
                return true;
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
        // 参数验证
        if (size <= 0) {
            return Collections.emptyList();
        }

        RankTimeEnum rankTimeEnum = RankTimeEnum.getEnumByValue(value);
        if (rankTimeEnum == null) {
            return Collections.emptyList();
        }

        String rankKey = rankTimeEnum == RankTimeEnum.DAY ? todayRankKey() : monthRankKey();

        // 1. 获取topN的活跃用户及其分数
        Set<ZSetOperations.TypedTuple<String>> userTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(rankKey, 0, size - 1);

        if (CollUtil.isEmpty(userTuples)) {
            return Collections.emptyList();
        }

        // 2. 提取用户ID并查询用户信息
        List<Long> userIds = userTuples.stream()
                .map(tuple -> Long.valueOf(tuple.getValue()))
                .collect(Collectors.toList());

        List<User> userList = userService.listByIds(userIds);
        if (CollUtil.isEmpty(userList)) {
            return Collections.emptyList();
        }

        // 创建用户ID到用户的映射
        Map<Long, User> userMap = userList.stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        // 3. 构建排行榜项
        List<UserActiveRankItemVO> rankItemList = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : userTuples) {
            Long userId = Long.valueOf(tuple.getValue());
            User user = userMap.get(userId);

            if (user != null) {
                UserActiveRankItemVO item = new UserActiveRankItemVO();
                item.setUser(userService.getUserVO(user));
                item.setScore(tuple.getScore());
                item.setRank(rank++);
                rankItemList.add(item);
            }
        }

        return rankItemList;
    }


}
