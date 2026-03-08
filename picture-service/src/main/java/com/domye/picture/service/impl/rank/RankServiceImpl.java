package com.domye.picture.service.impl.rank;

import cn.hutool.core.date.DateUtil;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.helper.impl.RedisCache;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.dto.rank.UserActivityScoreAddRequest;
import com.domye.picture.model.enums.ActivityScoreType;
import com.domye.picture.model.enums.RankTimeEnum;
import com.domye.picture.model.vo.rank.UserActiveRankItemVO;
import com.domye.picture.model.vo.rank.UserRankVO;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.service.cache.RankCacheService;
import com.domye.picture.service.mapper.PictureMapper;
import com.domye.picture.service.api.rank.RankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 活跃度排行榜服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankServiceImpl implements RankService {

    private final RedisCache redisCache;
    private final RankCacheService rankCacheService;
    private final PictureMapper pictureMapper;

    private static final String ACTIVITY_SCORE_KEY = "activity_rank_";

    @Override
    public Boolean addActivityScore(User user, UserActivityScoreAddRequest userActivityScoreAddRequest) {
        // 参数校验
        Throw.throwIf(user == null, ErrorCode.PARAMS_ERROR);
        long userId = user.getId();
        Throw.throwIf(userId <= 0, ErrorCode.PARAMS_ERROR);

        String field;
        int score = 0;

        // 根据请求类型确定分数
        if (userActivityScoreAddRequest.getPath() != null) {
            // 查看图片
            Picture picture = pictureMapper.selectById(userActivityScoreAddRequest.getPath());
            if (picture == null) {
                return true;
            }
            field = "path_" + userActivityScoreAddRequest.getPath();
            score = ActivityScoreType.VIEW_PICTURE.getScore();
        } else if (userActivityScoreAddRequest.getPictureId() != null) {
            Long pictureId = userActivityScoreAddRequest.getPictureId();
            field = pictureId + "_";

            if (BooleanUtils.isTrue(userActivityScoreAddRequest.getUploadPicture())) {
                // 发布图片
                field += "publish";
                score = ActivityScoreType.PUBLISH_PICTURE.getScore();
            } else if (BooleanUtils.isTrue(userActivityScoreAddRequest.getCommentPicture())) {
                // 评论图片
                field += "comment";
                score = ActivityScoreType.COMMENT_PICTURE.getScore();
            } else if (BooleanUtils.isTrue(userActivityScoreAddRequest.getLikePicture())) {
                // 点赞图片
                field += "like";
                score = ActivityScoreType.LIKE_PICTURE.getScore();
            } else if (BooleanUtils.isTrue(userActivityScoreAddRequest.getFavoritePicture())) {
                // 收藏图片
                field += "favorite";
                score = ActivityScoreType.FAVORITE_PICTURE.getScore();
            } else if (BooleanUtils.isTrue(userActivityScoreAddRequest.getSharePicture())) {
                // 分享图片
                field += "share";
                score = ActivityScoreType.SHARE_PICTURE.getScore();
            } else {
                // 其他行为（可扩展）
                return true;
            }
        } else {
            return true;
        }

        // 用户行为去重 Key（每天重置）
        String userActionKey = rankCacheService.getUserActionKey(userId);
        Object ansObj = redisCache.getHash(userActionKey, field);
        Integer ans = ansObj != null ? Integer.parseInt(String.valueOf(ansObj)) : null;

        // 如果该行为当天未记录，执行加分
        if (ans == null) {
            // 记录用户行为（防止重复加分）
            redisCache.putHash(userActionKey, field, score);
            redisCache.expireKey(userActionKey, 31, TimeUnit.DAYS);

            // 更新所有榜单分数
            boolean success = rankCacheService.addActivityScore(userId, score);

            if (log.isDebugEnabled()) {
                log.info("活跃度更新加分! userId = {}, field = {}, score = {}, success = {}",
                        userId, field, score, success);
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

        return rankCacheService.queryRankList(rankTimeEnum, size);
    }

    /**
     * 获取用户在各榜单的排名
     *
     * @param userId 用户ID
     * @return 用户排名信息
     */
    public UserRankVO getUserRank(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        UserRankVO userRankVO = new UserRankVO();
        userRankVO.setUserId(userId);

        // 获取各榜单排名和分数
        Map<String, Object> rankMap = new HashMap<>();
        for (RankTimeEnum rankTime : RankTimeEnum.values()) {
            long rank = rankCacheService.getUserRank(userId, rankTime);
            double score = rankCacheService.getUserScore(userId, rankTime);

            Map<String, Object> item = new HashMap<>();
            item.put("rank", rank);
            item.put("score", score);
            rankMap.put(rankTime.getName(), item);
        }
        userRankVO.setRanks(rankMap);

        return userRankVO;
    }
}