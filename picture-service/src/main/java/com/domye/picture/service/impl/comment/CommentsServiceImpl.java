package com.domye.picture.service.impl.comment;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.dto.comment.CommentQueryRequest;
import com.domye.picture.model.dto.comment.CommentReplyQueryRequest;
import com.domye.picture.model.entity.comment.Comments;
import com.domye.picture.model.entity.comment.CommentsContent;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.comment.CommentListVO;
import com.domye.picture.model.vo.comment.CommentReplyVO;
import com.domye.picture.service.api.comment.CommentsContentService;
import com.domye.picture.service.api.comment.CommentsService;
import com.domye.picture.service.api.picture.PictureService;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.helper.comment.DataMaps;
import com.domye.picture.service.helper.comment.IdCollection;
import com.domye.picture.service.mapper.CommentsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;
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
        Comments comment =Comments.builder()
                .pictureid(pictureId)
                .userid(userId)
                .parentid(parentId)
                .rootid(rootId)
                .replycount(0)
                .likecount(0)
                .build();
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
    public Page<CommentListVO> listTopCommentsWithPreview(CommentQueryRequest request) {
        long pictureId = request.getPictureId();
        int current = request.getCurrent();
        int pageSize = request.getPageSize();
        int replyPreviewLimit = request.getPreviewSize();

        Page<Comments> commentsPage = commentsMapper.selectPage(
                new Page<>(current, pageSize),
                new QueryWrapper<Comments>()
                        .eq("pictureId", pictureId)
                        .isNull("rootId")
                        .orderByDesc("createdTime")
        );

        if (commentsPage.getRecords().isEmpty()) {
            return new Page<>(current, pageSize, 0);
        }

        List<Comments> comments = commentsPage.getRecords();
        List<Long> rootIds = extractIds(comments, Comments::getCommentid);
        Map<Long, List<Comments>> repliesMap = fetchAndGroupReplies(rootIds, replyPreviewLimit);

        return processCommentPage(commentsPage, comments, repliesMap);
    }

    /**
     * 提取ID列表
     * @param items
     * @param mapper
     * @return
     * @param <T>
     */
    @Override
    public <T> List<Long> extractIds(List<T> items, Function<T, Long> mapper) {
        return items.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * 批量获取并分组楼中楼
     * @param rootIds
     * @param limit
     * @return
     */
    @Override
    public Map<Long, List<Comments>> fetchAndGroupReplies(List<Long> rootIds, int limit) {
        if (rootIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Comments> allReplies = commentsMapper.selectTopRepliesByRootIds(rootIds, limit);
        return allReplies.stream().collect(Collectors.groupingBy(Comments::getRootid));
    }

    /**
     * 收集所有需要的ID
     * @param firstLevelComments
     * @param repliesMap
     * @return
     */
    @Override
    public IdCollection collectAllIds(List<Comments> firstLevelComments, Map<Long, List<Comments>> repliesMap) {
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

    /**
     * 批量查询并构建映射
     * @param idCollection
     * @return
     */
    @Override
    public DataMaps buildDataMaps(IdCollection idCollection) {
        Map<Long, User> userMap = idCollection.getUserIds().isEmpty() ? Collections.emptyMap() :
                userService.listByIds(idCollection.getUserIds()).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, CommentsContent> contentMap = idCollection.getCommentIds().isEmpty() ? Collections.emptyMap() :
                commentsContentService.listByIds(idCollection.getCommentIds()).stream()
                        .collect(Collectors.toMap(CommentsContent::getCommentId, c -> c));

        return new DataMaps(userMap, contentMap);
    }

    /**
     * 构建评论VO列表
     * @param firstLevelComments
     * @param repliesMap
     * @param dataMaps
     * @return
     */
    @Override
    public List<CommentListVO> buildCommentVOList(List<Comments> firstLevelComments,
                                                  Map<Long, List<Comments>> repliesMap,
                                                  DataMaps dataMaps) {
        List<CommentListVO> commentListVOList = new ArrayList<>(firstLevelComments.size());

        for (Comments comment : firstLevelComments) {
            CommentListVO vo = buildCommentVO(comment, dataMaps);

            List<Comments> replies = repliesMap.getOrDefault(comment.getCommentid(), Collections.emptyList());
            List<CommentReplyVO> replyVOList = buildReplyVOList(replies, dataMaps);

            vo.setReplyPreviewList(replyVOList);
            commentListVOList.add(vo);
        }

        return commentListVOList;
    }

    /**
     * 构建评论VO
     * @param comment
     * @param dataMaps
     * @return
     */
    @Override
    public CommentListVO buildCommentVO(Comments comment, DataMaps dataMaps) {
        User user = dataMaps.getUserMap().get(comment.getUserid());
        CommentsContent content = dataMaps.getContentMap().get(comment.getCommentid());

        return CommentListVO.builder()
                .commentId(comment.getCommentid())
                .userId(comment.getUserid())
                .userName(user.getUserName())
                .userAvatar(user.getUserAvatar())
                .replyCount(comment.getReplycount())
                .createTime(comment.getCreatedtime())
                .content(content.getCommentText())
                .build();
    }

    /**
     * 构建楼中楼VO列表
     * @param replies
     * @param dataMaps
     * @return
     */
    @Override
    public List<CommentReplyVO> buildReplyVOList(List<Comments> replies, DataMaps dataMaps) {
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
                    .createTime(reply.getCreatedtime())
                    .content(replyContent.getCommentText())
                    .parentId(reply.getParentid())
//                    .parentUserName(parentUser.getUserName())
                    .build();
            replyVOList.add(replyVO);
        }

        return replyVOList;
    }

    /**
     * 验证评论请求
     * @param request
     * @param userId
     */
    @Override
    public void validateCommentRequest(CommentAddRequest request, Long userId) {
        Throw.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Throw.throwIf(request.getPictureid() == null, ErrorCode.PARAMS_ERROR, "图片ID不能为空");
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);

        String content = request.getContent();
        Throw.throwIf(content == null || content.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "评论内容不能为空");
        Throw.throwIf(content.length() > 500, ErrorCode.PARAMS_ERROR, "评论内容不能超过500字");
    }

    /**
     * 获取根评论ID
     * @param parentId
     * @return
     */
    @Override
    public Long resolveRootId(Long parentId) {
        if (parentId == null) {
            return null;
        }
        Comments parentComment = getById(parentId);
        Throw.throwIf(parentComment == null, ErrorCode.NOT_FOUND_ERROR, "父评论不存在");

        // 如果父评论有根评论，则使用父评论的根评论；否则父评论本身就是根评论
        return parentComment.getRootid() != null ? parentComment.getRootid() : parentId;
    }

    /**
     * 获取楼中楼评论列表
     * @param request
     * @return
     */
    @Override
    public Page<CommentListVO> listReplyComments(CommentReplyQueryRequest request) {
        long pictureId = request.getPictureId();
        long rootCommentId = request.getCommentId();
        int current = request.getCurrent();
        int pageSize = request.getPageSize();

        Comments rootComment = getById(rootCommentId);
        if (rootComment == null) {
            return new Page<>(current, pageSize, 0);
        }

        Page<Comments> repliesPage = commentsMapper.selectPage(
                new Page<>(current, pageSize),
                new QueryWrapper<Comments>()
                        .eq("pictureId", pictureId)
                        .eq("rootId", rootCommentId)
                        .orderByAsc("createdTime")
        );

        List<Comments> replies = repliesPage.getRecords();

        Map<Long, List<Comments>> repliesMap = new HashMap<>();
        repliesMap.put(rootCommentId, replies);

        List<Comments> rootCommentList = Collections.singletonList(rootComment);

        return processCommentPage(repliesPage, rootCommentList, repliesMap);
    }

    /**
     * 处理评论分页
     * @param commentsPage
     * @param comments
     * @param repliesMap
     * @return
     */
    private Page<CommentListVO> processCommentPage(Page<Comments> commentsPage,
                                                   List<Comments> comments,
                                                   Map<Long, List<Comments>> repliesMap) {
        IdCollection idCollection = collectAllIds(comments, repliesMap);
        DataMaps dataMaps = buildDataMaps(idCollection);
        List<CommentListVO> commentListVOList = buildCommentVOList(comments, repliesMap, dataMaps);

        Page<CommentListVO> resultPage = new Page<>(
                commentsPage.getCurrent(),
                commentsPage.getSize(),
                commentsPage.getTotal()
        );
        resultPage.setRecords(commentListVOList);
        return resultPage;
    }
}
