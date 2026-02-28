package com.domye.picture.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.comment.CommentAddRequest;
import com.domye.picture.model.dto.comment.CommentQueryRequest;
import com.domye.picture.model.dto.comment.CommentReplyQueryRequest;
import com.domye.picture.model.dto.contact.ContactQueryRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.comment.CommentListVO;
import com.domye.picture.model.vo.contact.ContactVO;
import com.domye.picture.service.api.comment.CommentsContentService;
import com.domye.picture.service.api.comment.CommentsService;
import com.domye.picture.service.api.contact.ContactService;
import com.domye.picture.service.api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/comment")
@RequiredArgsConstructor
public class CommentController {
    final CommentsService commentService;
    final CommentsContentService commentContentService;
    final UserService userService;
    final ContactService contactService;

    /**
     * 添加评论
     *
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
    public BaseResponse<Page<CommentListVO>> listTopComments(CommentQueryRequest request) {
        return Result.success(commentService.listTopCommentsWithPreview(request));
    }

    @GetMapping("/reply")
    public BaseResponse<Page<CommentListVO>> listReplyComments(CommentReplyQueryRequest request) {
        return Result.success(commentService.listReplyComments(request));
    }

    @GetMapping("/friends")
    @Operation(summary = "获取好友列表 (用于@选择器)")
    public BaseResponse<List<Map<String, Object>>> getFriends(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Throw.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ContactQueryRequest queryRequest = new ContactQueryRequest();
        queryRequest.setCurrent(1);
        queryRequest.setPageSize(1000);
        queryRequest.setStatus(1); // 仅返回已通过的好友
        Page<ContactVO> contactPage = contactService.getMyContacts(queryRequest, loginUser.getId());
        List<Map<String, Object>> friends = contactPage.getRecords().stream()
                .map(contact -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", contact.getContactUserId());
                    if (contact.getContactUser() != null) {
                        map.put("userName", contact.getContactUser().getUserName());
                        map.put("userAvatar", contact.getContactUser().getUserAvatar());
                    }
                    return map;
                })
                .collect(Collectors.toList());

        return Result.success(friends);
    }
}