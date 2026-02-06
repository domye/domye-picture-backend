package com.domye.picture.service.comment;

import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.entity.comment.Comments;

import javax.servlet.http.HttpServletRequest;

/**
* @author Domye
* @description 针对表【comments】的数据库操作Service
* @createDate 2026-02-06 12:43:06
*/
public interface CommentsService extends IService<Comments> {

    Long addComment(CommentAddRequest commentAddRequest, Long userId, HttpServletRequest request);
}
