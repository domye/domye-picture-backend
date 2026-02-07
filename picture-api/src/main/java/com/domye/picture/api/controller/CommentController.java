package com.domye.picture.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.dto.comment.CommentQueryRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.comment.CommentVO;
import com.domye.picture.service.api.comment.CommentsContentService;
import com.domye.picture.service.api.comment.CommentsService;
import com.domye.picture.service.api.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    final CommentsService commentService;
    final CommentsContentService commentContentService;
    final UserService userService;

    /**
     * 添加评论
     * @param commentAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addComment(@RequestBody CommentAddRequest commentAddRequest,
                                         HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        Throw.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);

        Long commentId = commentService.addComment(commentAddRequest, user.getId(), request);
        return Result.success(commentId);
    }

    @GetMapping("/list")
    public BaseResponse<Page<CommentVO>> listTopComments(CommentQueryRequest request) {
        return Result.success(commentService.listTopCommentsWithPreview(request));
    }
}