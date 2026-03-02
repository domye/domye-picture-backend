package com.domye.picture.service.impl.feed;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.feed.FeedQueryRequest;
import com.domye.picture.model.entity.comment.Comments;
import com.domye.picture.model.entity.contact.Contact;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.entity.space.SpaceUser;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.ContactStatusEnum;
import com.domye.picture.model.enums.FeedTypeEnum;
import com.domye.picture.model.enums.PictureReviewStatusEnum;
import com.domye.picture.model.mapper.picture.PictureStructMapper;
import com.domye.picture.model.mapper.user.UserStructMapper;
import com.domye.picture.model.vo.feed.FeedVO;
import com.domye.picture.model.vo.picture.PictureVO;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.api.contact.ContactService;
import com.domye.picture.service.api.feed.FeedService;
import com.domye.picture.service.api.picture.PictureService;
import com.domye.picture.service.api.space.SpaceService;
import com.domye.picture.service.api.space.SpaceUserService;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.mapper.CommentsMapper;
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
    private final ContactService contactService;
    private final SpaceService spaceService;
    private final SpaceUserService spaceUserService;
    private final UserService userService;
    private final CommentsMapper commentsMapper;
    private final PictureStructMapper pictureStructMapper;
    private final UserStructMapper userStructMapper;

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

        // 解析游标
        CursorInfo cursorInfo = parseCursor(feedQueryRequest.getCursor());

        // 根据类型获取不同的信息流
        List<Picture> pictureList;
        switch (typeEnum) {
            case FOLLOW:
                pictureList = getFollowFeed(loginUser, cursorInfo, size + 1);
                break;
            case RECOMMEND:
                pictureList = getRecommendFeed(cursorInfo, size + 1);
                break;
            case LATEST:
            default:
                pictureList = getLatestFeed(cursorInfo, size + 1);
                break;
        }

        // 构建响应
        return buildFeedVO(pictureList, size, loginUser);
    }

    /**
     * 获取关注流
     * 展示用户关注的人发布的图片
     */
    private List<Picture> getFollowFeed(User loginUser, CursorInfo cursorInfo, int limit) {
        if (loginUser == null) {
            return Collections.emptyList();
        }

        // 获取用户关注的人的ID列表
        List<Contact> contacts = contactService.lambdaQuery()
                .eq(Contact::getUserId, loginUser.getId())
                .eq(Contact::getStatus, ContactStatusEnum.ACCEPTED.getValue())
                .list();

        if (CollUtil.isEmpty(contacts)) {
            return Collections.emptyList();
        }

        List<Long> followUserIds = contacts.stream()
                .map(Contact::getContactUserId)
                .collect(Collectors.toList());

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
     * 基于热度排序（点赞数 * 3 + 评论数 * 5）
     */
    private List<Picture> getRecommendFeed(CursorInfo cursorInfo, int limit) {
        // 获取所有审核通过的图片
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("reviewStatus", PictureReviewStatusEnum.PASS.getValue())
                .isNull("spaceId"); // 只推荐公开图片

        // 游标条件
        applyCursorCondition(queryWrapper, cursorInfo);

        // 先获取图片列表
        List<Picture> pictures = pictureService.list(queryWrapper);

        if (CollUtil.isEmpty(pictures)) {
            return Collections.emptyList();
        }

        // 获取图片ID列表
        List<Long> pictureIds = pictures.stream()
                .map(Picture::getId)
                .collect(Collectors.toList());

        // 获取评论数统计
        Map<Long, Integer> commentCountMap = getCommentCountMap(pictureIds);

        // 计算热度并排序
        List<Picture> sortedPictures = pictures.stream()
                .sorted((p1, p2) -> {
                    int score1 = calculateHotScore(p1, commentCountMap.getOrDefault(p1.getId(), 0));
                    int score2 = calculateHotScore(p2, commentCountMap.getOrDefault(p2.getId(), 0));
                    return Integer.compare(score2, score1); // 热度降序
                })
                .limit(limit)
                .collect(Collectors.toList());

        return sortedPictures;
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
     * 获取图片评论数统计
     */
    private Map<Long, Integer> getCommentCountMap(List<Long> pictureIds) {
        if (CollUtil.isEmpty(pictureIds)) {
            return Collections.emptyMap();
        }

        // 查询每个图片的评论数
        QueryWrapper<Comments> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("pictureid", pictureIds)
                .isNull("parentid") // 只统计根评论
                .select("pictureid", "COUNT(*) as count")
                .groupBy("pictureid");

        List<Comments> comments = commentsMapper.selectList(queryWrapper);

        return comments.stream()
                .collect(Collectors.toMap(
                        Comments::getPictureid,
                        c -> c.getReplycount() != null ? c.getReplycount() : 0,
                        (v1, v2) -> v1
                ));
    }

    /**
     * 计算热度分数
     * 热度 = 评论数 * 5
     * 后续可扩展：点赞数 * 3 + 浏览量
     */
    private int calculateHotScore(Picture picture, int commentCount) {
        return commentCount * 5;
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
     * 构建信息流响应
     */
    private FeedVO buildFeedVO(List<Picture> pictureList, int size, User loginUser) {
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

        // 设置下一页游标
        if (hasMore && !pictureList.isEmpty()) {
            Picture lastPicture = pictureList.get(pictureList.size() - 1);
            String nextCursor = lastPicture.getEditTime().getTime() + "_" + lastPicture.getId();
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
                .collect(Collectors.toList());

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
                    .collect(Collectors.toList());

            result.addAll(accessiblePrivatePictures);
        }

        return result;
    }

    /**
     * 填充用户信息
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

        // 批量查询用户
        List<User> users = userService.listByIds(userIds);
        if (CollUtil.isEmpty(users)) {
            return;
        }

        Map<Long, UserVO> userVOMap = users.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        userStructMapper::toUserVo
                ));

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
    private static class CursorInfo {
        private final Date editTime;
        private final Long id;

        public CursorInfo(Date editTime, Long id) {
            this.editTime = editTime;
            this.id = id;
        }

        public Date getEditTime() {
            return editTime;
        }

        public Long getId() {
            return id;
        }
    }
}