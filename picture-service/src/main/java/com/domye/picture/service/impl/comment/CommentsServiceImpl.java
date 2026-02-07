package com.domye.picture.service.impl.comment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.dto.comment.CommentQueryRequest;
import com.domye.picture.model.entity.comment.Comments;
import com.domye.picture.model.entity.comment.CommentsContent;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.comment.CommentReplyVO;
import com.domye.picture.model.vo.comment.CommentVO;
import com.domye.picture.service.api.comment.CommentsContentService;
import com.domye.picture.service.api.comment.CommentsService;
import com.domye.picture.service.mapper.CommentsMapper;
import com.domye.picture.service.api.picture.PictureService;
import com.domye.picture.service.api.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments>
        implements CommentsService {

    final PictureService pictureService;
    final CommentsContentService commentsContentService;
    final CommentsMapper commentsMapper;
    final UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addComment(CommentAddRequest request, Long userId, HttpServletRequest httpRequest) {
        // 1. 参数校验
        validateCommentRequest(request, userId);

        Long pictureId = request.getPictureid();
        Long parentId = request.getParentid();

        // 2. 验证图片存在
        Picture picture = pictureService.getById(pictureId);
        Throw.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

        // 3. 处理楼中楼逻辑，获取根评论ID
        Long rootId = resolveRootId(parentId);

        // 4. 保存评论记录
        Comments comment = new Comments();
        comment.setPictureid(pictureId);
        comment.setUserid(userId);
        comment.setParentid(parentId);
        comment.setRootid(rootId);
        comment.setReplycount(0);
        comment.setLikecount(0);
        save(comment);

        // 5. 保存评论内容
        CommentsContent content = new CommentsContent();
        content.setCommentId(comment.getCommentid());
        content.setCommentText(request.getContent());
        commentsContentService.save(content);

        // 6. 更新父评论回复数（如果是楼中楼）
        if (parentId != null) {
            commentsMapper.incrementReplyCount(rootId);
        }

        return comment.getCommentid();
    }

    @Override
    public Page<CommentVO> listTopCommentsWithPreview(CommentQueryRequest request) {
        long pictureId = request.getPictureId();
        int current = (int) request.getCurrent();
        int pageSize = (int) request.getPageSize();
        int replyPreviewLimit = request.getPreviewSize();

        Page<Comments> firstLevelPage = commentsMapper.selectPage(
                new Page<>(current, pageSize),
                new QueryWrapper<Comments>()
                        .eq("pictureId", pictureId)
                        .isNull("rootId")
                        .orderByDesc("createdTime")
        );

        if (firstLevelPage.getRecords().isEmpty()) {
            return new Page<>(current, pageSize, 0);
        }

        List<Comments> firstLevelComments = firstLevelPage.getRecords();
        List<Long> rootIds = extractIds(firstLevelComments, Comments::getCommentid);

        Map<Long, List<Comments>> repliesMap = fetchAndGroupReplies(rootIds, replyPreviewLimit);
        IdCollection idCollection = collectAllIds(firstLevelComments, repliesMap);
        DataMaps dataMaps = buildDataMaps(idCollection);
        List<CommentVO> commentVOList = buildCommentVOList(firstLevelComments, repliesMap, dataMaps);

        Page<CommentVO> resultPage = new Page<>(current, pageSize, firstLevelPage.getTotal());
        resultPage.setRecords(commentVOList);
        return resultPage;
    }

    private <T> List<Long> extractIds(List<T> items, java.util.function.Function<T, Long> mapper) {
        return items.stream().map(mapper).collect(Collectors.toList());
    }

    private Map<Long, List<Comments>> fetchAndGroupReplies(List<Long> rootIds, int limit) {
        if (rootIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Comments> allReplies = commentsMapper.selectTopRepliesByRootIds(rootIds, limit);
        return allReplies.stream().collect(Collectors.groupingBy(Comments::getRootid));
    }

    private IdCollection collectAllIds(List<Comments> firstLevelComments, Map<Long, List<Comments>> repliesMap) {
        Set<Long> allUserIds = new HashSet<>(firstLevelComments.size() * 2);
        Set<Long> allCommentIds = new HashSet<>(firstLevelComments.size());

        for (Comments comment : firstLevelComments) {
            allUserIds.add(comment.getUserid());
            allCommentIds.add(comment.getCommentid());

            List<Comments> replies = repliesMap.get(comment.getCommentid());
            if (replies != null) {
                for (Comments reply : replies) {
                    allUserIds.add(reply.getUserid());
                    allCommentIds.add(reply.getCommentid());
                    if (reply.getParentid() != null) {
                        allUserIds.add(reply.getParentid());
                    }
                }
            }
        }

        return new IdCollection(allUserIds, allCommentIds);
    }

    private DataMaps buildDataMaps(IdCollection idCollection) {
        Map<Long, User> userMap = idCollection.getUserIds().isEmpty() ? Collections.emptyMap() :
                userService.listByIds(idCollection.getUserIds()).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, CommentsContent> contentMap = idCollection.getCommentIds().isEmpty() ? Collections.emptyMap() :
                commentsContentService.listByIds(idCollection.getCommentIds()).stream()
                        .collect(Collectors.toMap(CommentsContent::getCommentId, c -> c));

        return new DataMaps(userMap, contentMap);
    }

    private List<CommentVO> buildCommentVOList(List<Comments> firstLevelComments,
                                                Map<Long, List<Comments>> repliesMap,
                                                DataMaps dataMaps) {
        List<CommentVO> commentVOList = new ArrayList<>(firstLevelComments.size());

        for (Comments comment : firstLevelComments) {
            CommentVO vo = buildCommentVO(comment, dataMaps);

            List<Comments> replies = repliesMap.getOrDefault(comment.getCommentid(), Collections.emptyList());
            List<CommentReplyVO> replyVOList = buildReplyVOList(replies, dataMaps);

            vo.setReplyPreviewList(replyVOList);
            commentVOList.add(vo);
        }

        return commentVOList;
    }

    private CommentVO buildCommentVO(Comments comment, DataMaps dataMaps) {
        User user = dataMaps.getUserMap().get(comment.getUserid());
        CommentsContent content = dataMaps.getContentMap().get(comment.getCommentid());

        return CommentVO.builder()
                .commentId(comment.getCommentid())
                .userId(comment.getUserid())
                .userName(user.getUserName())
                .userAvatar(user.getUserAvatar())
                .replyCount(comment.getReplycount())
                .createTime(comment.getCreatedtime())
                .content(content.getCommentText())
                .build();
    }

    private List<CommentReplyVO> buildReplyVOList(List<Comments> replies, DataMaps dataMaps) {
        List<CommentReplyVO> replyVOList = new ArrayList<>(replies.size());

        for (Comments reply : replies) {
            User replyUser = dataMaps.getUserMap().get(reply.getUserid());
            User parentUser = dataMaps.getUserMap().get(reply.getParentid());
            CommentsContent replyContent = dataMaps.getContentMap().get(reply.getCommentid());

            CommentReplyVO replyVO = CommentReplyVO.builder()
                    .commentId(reply.getCommentid())
                    .userId(reply.getUserid())
                    .userName(replyUser.getUserName())
                    .userAvatar(replyUser.getUserAvatar())
                    .content(replyContent.getCommentText())
                    .parentId(reply.getParentid())
                    .parentUserName(parentUser.getUserName())
                    .build();
            replyVOList.add(replyVO);
        }

        return replyVOList;
    }

    private static class IdCollection {
        private final Set<Long> userIds;
        private final Set<Long> commentIds;

        public IdCollection(Set<Long> userIds, Set<Long> commentIds) {
            this.userIds = userIds;
            this.commentIds = commentIds;
        }

        public Set<Long> getUserIds() {
            return userIds;
        }

        public Set<Long> getCommentIds() {
            return commentIds;
        }
    }

    private static class DataMaps {
        private final Map<Long, User> userMap;
        private final Map<Long, CommentsContent> contentMap;

        public DataMaps(Map<Long, User> userMap, Map<Long, CommentsContent> contentMap) {
            this.userMap = userMap;
            this.contentMap = contentMap;
        }

        public Map<Long, User> getUserMap() {
            return userMap;
        }

        public Map<Long, CommentsContent> getContentMap() {
            return contentMap;
        }
    }


    private void validateCommentRequest(CommentAddRequest request, Long userId) {
        Throw.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Throw.throwIf(request.getPictureid() == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);

        String content = request.getContent();
        Throw.throwIf(content == null || content.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        Throw.throwIf(content.length() > 500, ErrorCode.PARAMS_ERROR, "评论内容不能超过500字");
    }

    private Long resolveRootId(Long parentId) {
        if (parentId == null) {
            return null;
        }
        Comments parentComment = getById(parentId);
        Throw.throwIf(parentComment == null, ErrorCode.NOT_FOUND_ERROR, "父评论不存在");

        // 如果父评论有根评论，则使用父评论的根评论；否则父评论本身就是根评论
        return parentComment.getRootid() != null ? parentComment.getRootid() : parentId;
    }
}
