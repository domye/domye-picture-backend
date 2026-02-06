package com.domye.picture.service.comment.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.entity.comment.Comments;
import com.domye.picture.model.entity.comment.CommentsContent;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.service.comment.CommentsContentService;
import com.domye.picture.service.comment.CommentsService;
import com.domye.picture.service.mapper.CommentsMapper;
import com.domye.picture.service.picture.PictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

@Service
@RequiredArgsConstructor
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments>
        implements CommentsService {

    final PictureService pictureService;
    final CommentsContentService commentsContentService;
    final CommentsMapper commentsMapper;

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
