package com.domye.picture.api.controller;

import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.service.comment.CommentsContentService;
import com.domye.picture.service.comment.CommentsService;
import com.domye.picture.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    final CommentsService commentService;
    final CommentsContentService commentContentService;
    final UserService userService;

    //添加评论功能，需要图片id，用户id，评论内容，
    // 而楼中楼回复，需要父评论id，根评论id
    @PostMapping("/add")
    public BaseResponse<Boolean> addComment(@RequestBody CommentAddRequest commentAddRequest, HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        Throw.throwIf(user==null,ErrorCode.NOT_LOGIN_ERROR);
        Throw.throwIf(commentAddRequest == null, ErrorCode.PARAMS_ERROR);
        String content = commentAddRequest.getContent();
        Throw.throwIf(content == null, ErrorCode.PARAMS_ERROR);
        Long commentId = commentService.addComment(commentAddRequest,user.getId(),request);
        commentContentService.addCommentContent(commentId, content);
        return Result.success(true);
    }
}