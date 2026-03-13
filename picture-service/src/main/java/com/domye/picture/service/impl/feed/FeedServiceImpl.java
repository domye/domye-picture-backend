package com.domye.picture.service.impl.feed;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.feed.FeedQueryRequest;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.entity.space.SpaceUser;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.FeedTypeEnum;
import com.domye.picture.model.enums.PictureReviewStatusEnum;
import com.domye.picture.model.mapper.picture.PictureStructMapper;
import com.domye.picture.model.vo.feed.FeedVO;
import com.domye.picture.model.vo.picture.PictureVO;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.api.feed.FeedService;
import com.domye.picture.service.api.picture.PictureService;
import com.domye.picture.service.api.space.SpaceUserService;
import com.domye.picture.service.cache.FeedCacheService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 信息流服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final PictureService pictureService;
    private final SpaceUserService spaceUserService;
    private final FeedCacheService feedCacheService;
    private final PictureStructMapper pictureStructMapper;

    /**
     * 默认每页数量
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大每页数量
     */
    private static final int MAX_PAGE_SIZE = 50;

    @Override
    public FeedVO getFeed(FeedQueryRequest feedQueryRequest, User loginUser) {
        // 参数校验
        Throw.throwIf(feedQueryRequest == null, ErrorCode.PARAMS_ERROR);
        FeedTypeEnum typeEnum = feedQueryRequest.getTypeEnum();
        Throw.throwIf(typeEnum == null, ErrorCode.PARAMS_ERROR, "信息流类型无效");

        // 限制每页数量
        Integer requestSize = feedQueryRequest.getSize();
        int size = DEFAULT_PAGE_SIZE;
        if (requestSize != null && requestSize > 0) {
            size = requestSize;
        }
        size = Math.min(size, MAX_PAGE_SIZE);

        // 根据类型获取不同的信息流
        List<Picture> pictureList;
        switch (typeEnum) {
            case FOLLOW:
                CursorInfo followCursor = parseCursor(feedQueryRequest.getCursor());
                pictureList = getFollowFeed(loginUser, followCursor, size + 1);
                break;
            case RECOMMEND:
                RecommendCursorInfo recommendCursor = parseRecommendCursor(feedQueryRequest.getCursor());
                pictureList = getRecommendFeed(recommendCursor, size + 1);
                break;
            case LATEST:
            default:
                CursorInfo latestCursor = parseCursor(feedQueryRequest.getCursor());
                pictureList = getLatestFeed(latestCursor, size + 1);
                break;
        }

        // 构建响应
        return buildFeedVO(pictureList, size, loginUser, typeEnum);
    }

    /**
     * 获取关注流
     * 展示用户关注的人发布的图片
     */
    private List<Picture> getFollowFeed(User loginUser, CursorInfo cursorInfo, int limit) {
        if (loginUser == null) {
            return Collections.emptyList();
        }

        // 使用缓存获取用户关注列表
        List<Long> followUserIds = feedCacheService.getUserFollows(loginUser.getId());

        if (CollUtil.isEmpty(followUserIds)) {
            return Collections.emptyList();
        }

        // 构建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("userId", followUserIds)
                .eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue());

        // 游标条件
        applyCursorCondition(queryWrapper, cursorInfo);

        // 按编辑时间倒序
        queryWrapper.orderByDesc("editTime", "id")
                .last("LIMIT " + limit);

        return pictureService.list(queryWrapper);
    }

    /**
     * 获取推荐流
     * 基于热度排序，直接使用数据库索引优化查询
     * 热度分数由定时任务预先计算
     */
    private List<Picture> getRecommendFeed(RecommendCursorInfo cursorInfo, int limit) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue())
                .isNull("spaceId"); // 只推荐公开图片

        // 应用热度游标条件
        if (cursorInfo != null && cursorInfo.getHotScore() != null && cursorInfo.getId() != null) {
            queryWrapper.and(qw -> qw
                    .lt("hotScore", cursorInfo.getHotScore())
                    .or()
                    .eq("hotScore", cursorInfo.getHotScore())
                    .lt("id", cursorInfo.getId())
            );
        }

        // 按热度倒序排列，热度相同则按ID倒序
        queryWrapper.orderByDesc("hotScore", "id")
                .last("LIMIT " + limit);

        return pictureService.list(queryWrapper);
    }

    /**
     * 获取最新流
     * 按编辑时间倒序排列
     */
    private List<Picture> getLatestFeed(CursorInfo cursorInfo, int limit) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue());

        // 游标条件
        applyCursorCondition(queryWrapper, cursorInfo);

        // 按编辑时间倒序
        queryWrapper.orderByDesc("editTime", "id")
                .last("LIMIT " + limit);

        return pictureService.list(queryWrapper);
    }

    /**
     * 应用游标条件
     */
    private void applyCursorCondition(QueryWrapper<Picture> queryWrapper, CursorInfo cursorInfo) {
        if (cursorInfo != null && cursorInfo.getEditTime() != null && cursorInfo.getId() != null) {
            queryWrapper.and(qw -> qw
                    .lt("editTime", cursorInfo.getEditTime())
                    .or()
                    .eq("editTime", cursorInfo.getEditTime())
                    .lt("id", cursorInfo.getId())
            );
        }
    }

    /**
     * 解析游标
     * 格式: editTime_id
     */
    private CursorInfo parseCursor(String cursor) {
        if (StrUtil.isBlank(cursor)) {
            return null;
        }

        try {
            String[] parts = cursor.split("_");
            if (parts.length != 2) {
                return null;
            }
            long timestamp = Long.parseLong(parts[0]);
            Long id = Long.parseLong(parts[1]);
            return new CursorInfo(new Date(timestamp), id);
        } catch (Exception e) {
            log.warn("解析游标失败: {}", cursor, e);
            return null;
        }
    }

    /**
     * 解析推荐流游标
     * 格式: hotScore_id
     */
    private RecommendCursorInfo parseRecommendCursor(String cursor) {
        if (StrUtil.isBlank(cursor)) {
            return null;
        }

        try {
            String[] parts = cursor.split("_");
            if (parts.length != 2) {
                return null;
            }
            Integer hotScore = Integer.parseInt(parts[0]);
            Long id = Long.parseLong(parts[1]);
            return new RecommendCursorInfo(hotScore, id);
        } catch (Exception e) {
            log.warn("解析推荐流游标失败: {}", cursor, e);
            return null;
        }
    }

    /**
     * 构建信息流响应
     */
    private FeedVO buildFeedVO(List<Picture> pictureList, int size, User loginUser, FeedTypeEnum typeEnum) {
        FeedVO feedVO = new FeedVO();

        if (CollUtil.isEmpty(pictureList)) {
            feedVO.setRecords(Collections.emptyList());
            feedVO.setNextCursor(null);
            feedVO.setHasMore(false);
            return feedVO;
        }

        // 判断是否有更多数据
        boolean hasMore = pictureList.size() > size;
        if (hasMore) {
            pictureList = pictureList.subList(0, size);
        }

        // 过滤无权限的图片
        List<Picture> filteredPictures = filterPicturesByPermission(pictureList, loginUser);

        // 转换为VO
        List<PictureVO> pictureVOList = pictureStructMapper.toVoList(filteredPictures);

        // 填充用户信息
        fillUserInfo(pictureVOList, filteredPictures);

        // 构建响应
        feedVO.setRecords(pictureVOList);
        feedVO.setHasMore(hasMore);

        // 根据信息流类型设置下一页游标
        if (hasMore && !filteredPictures.isEmpty()) {
            Picture lastPicture = filteredPictures.get(filteredPictures.size() - 1);
            String nextCursor;
            if (typeEnum == FeedTypeEnum.RECOMMEND) {
                // 推荐流使用热度游标
                nextCursor = lastPicture.getHotScore() + "_" + lastPicture.getId();
            } else {
                // 关注流和最新流使用时间游标
                nextCursor = lastPicture.getEditTime().getTime() + "_" + lastPicture.getId();
            }
            feedVO.setNextCursor(nextCursor);
        } else {
            feedVO.setNextCursor(null);
        }

        return feedVO;
    }

    /**
     * 根据权限过滤图片
     */
    private List<Picture> filterPicturesByPermission(List<Picture> pictureList, User loginUser) {
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }

        // 分离公开图片和私有空间图片
        List<Picture> publicPictures = pictureList.stream()
                .filter(p -> p.getSpaceId() == null)
                .toList();

        List<Picture> privatePictures = pictureList.stream()
                .filter(p -> p.getSpaceId() != null)
                .collect(Collectors.toList());

        // 公开图片全部可见
        List<Picture> result = new ArrayList<>(publicPictures);

        // 处理私有空间图片
        if (CollUtil.isNotEmpty(privatePictures) && loginUser != null) {
            // 获取用户所属的空间ID
            List<SpaceUser> spaceUsers = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getUserId, loginUser.getId())
                    .list();

            Set<Long> userSpaceIds = spaceUsers.stream()
                    .map(SpaceUser::getSpaceId)
                    .collect(Collectors.toSet());

            // 过滤有权限的私有图片
            List<Picture> accessiblePrivatePictures = privatePictures.stream()
                    .filter(p -> userSpaceIds.contains(p.getSpaceId()))
                    .toList();

            result.addAll(accessiblePrivatePictures);
        }

        return result;
    }

    /**
     * 填充用户信息（使用缓存）
     */
    private void fillUserInfo(List<PictureVO> pictureVOList, List<Picture> pictureList) {
        if (CollUtil.isEmpty(pictureList)) {
            return;
        }

        // 获取所有用户ID
        Set<Long> userIds = pictureList.stream()
                .map(Picture::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (CollUtil.isEmpty(userIds)) {
            return;
        }

        // 使用缓存批量获取用户信息
        Map<Long, UserVO> userVOMap = feedCacheService.batchGetUserInfo(userIds);

        // 设置用户信息
        for (int i = 0; i < pictureVOList.size(); i++) {
            PictureVO pictureVO = pictureVOList.get(i);
            Long userId = pictureList.get(i).getUserId();
            if (userId != null && userVOMap.containsKey(userId)) {
                pictureVO.setUser(userVOMap.get(userId));
            }
        }
    }

    /**
     * 游标信息
     */
    @Getter
    private static class CursorInfo {
        private final Date editTime;
        private final Long id;

        public CursorInfo(Date editTime, Long id) {
            this.editTime = editTime;
            this.id = id;
        }

    }

    /**
     * 推荐流游标信息
     */
    @Getter
    private static class RecommendCursorInfo {
        private final Integer hotScore;
        private final Long id;

        public RecommendCursorInfo(Integer hotScore, Long id) {
            this.hotScore = hotScore;
            this.id = id;
        }

    }
}