package com.domye.picture.service.comment.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.entity.comment.Comments;
import com.domye.picture.model.entity.picture.Picture;
import com.domye.picture.service.comment.CommentsService;
import com.domye.picture.service.mapper.CommentsMapper;
import com.domye.picture.service.picture.PictureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Domye
 * @description 针对表【comments】的数据库操作Service实现
 * @createDate 2026-02-06 12:43:06
 */
@Service
@RequiredArgsConstructor
public class CommentsServiceImpl extends ServiceImpl<CommentsMapper, Comments>
        implements CommentsService {
    final PictureService pictureService;

    @Override
    public Long addComment(CommentAddRequest commentAddRequest, Long userId, HttpServletRequest request) {
        //检查参数是否完整，一定要包含图片id，用户id，评论内容
        Long pictureId = commentAddRequest.getPictureid();

        Throw.throwIf(pictureId == null || userId == null, ErrorCode.PARAMS_ERROR);

        //判断pictureID是否存在，不存在则返回错误
        Picture picture = pictureService.getById(pictureId);
        Throw.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        //检查是否含有父评论id，如果有，则为楼中楼评论，否则为普通评论
        //有父评论则获取父评论的根评论，然后设置当前的根评论为父评论的根评论
        //若无父评论，则设置当前的根评论为空
        Long parentId = commentAddRequest.getParentid();
        Long rootId = commentAddRequest.getRootid();
        if (parentId != null) {
            Comments parentComment = this.getById(parentId);
            Throw.throwIf(parentComment == null, ErrorCode.NOT_FOUND_ERROR);
            rootId = parentComment.getRootid();
            if(rootId==null)
                rootId=parentId;
        }
        Comments comments = new Comments();
        comments.setPictureid(pictureId);
        comments.setUserid(userId);
        comments.setParentid(parentId);
        comments.setRootid(rootId);
        save(comments);
        return comments.getCommentid();
    }
}




