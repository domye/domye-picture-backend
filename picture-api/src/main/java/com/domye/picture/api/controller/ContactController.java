package com.domye.picture.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.DeleteRequest;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.contact.ContactAddRequest;
import com.domye.picture.model.dto.contact.ContactHandleRequest;
import com.domye.picture.model.dto.contact.ContactQueryRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.contact.ContactVO;
import com.domye.picture.service.api.contact.ContactService;
import com.domye.picture.service.api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 联系人管理
 */
@RestController
@RequestMapping("/contact")
@Slf4j
@RequiredArgsConstructor
public class ContactController {

    final ContactService contactService;
    final UserService userService;

    /**
     * 发起好友申请
     */
    @PostMapping("/apply")
    @Operation(summary = "发起好友申请")
    public BaseResponse<Long> applyContact(@RequestBody ContactAddRequest request, HttpServletRequest httpServletRequest) {
        // 获取当前登录用户ID

        User loginUser = userService.getLoginUser(httpServletRequest);
        Long userId=loginUser.getId();
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        // 参数校验
        Throw.throwIf(request == null || request.getContactUserId() == null, ErrorCode.PARAMS_ERROR);
        // 调用Service
        long contactId = contactService.applyContact(request, userId);
        return Result.success(contactId);
    }

    /**
     * 处理好友申请
     */
    @PostMapping("/handle")
    @Operation(summary = "处理好友申请")
    public BaseResponse<Boolean> handleContactRequest(@RequestBody ContactHandleRequest request, HttpServletRequest httpServletRequest) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long userId=loginUser.getId();
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        // 参数校验
        Throw.throwIf(request == null || request.getId() == null, ErrorCode.PARAMS_ERROR);
        Throw.throwIf(request.getStatus() == null || request.getStatus().trim().isEmpty(), ErrorCode.PARAMS_ERROR);
        // 调用Service
        boolean result = contactService.handleContactRequest(request, userId);
        return Result.success(result);
    }

    /**
     * 查询我的联系人列表
     */
    @PostMapping("/list")
    @Operation(summary = "查询我的联系人列表")
    public BaseResponse<Page<ContactVO>> listContacts(@RequestBody ContactQueryRequest request, HttpServletRequest httpServletRequest) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long userId=loginUser.getId();
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        // 参数校验
        Throw.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 调用Service
        Page<ContactVO> contactPage = contactService.getMyContacts(request, userId);
        return Result.success(contactPage);
    }

    /**
     * 查询待处理申请
     */
    @PostMapping("/pending")
    @Operation(summary = "查询待处理申请")
    public BaseResponse<Page<ContactVO>> listPendingRequests(@RequestBody ContactQueryRequest request, HttpServletRequest httpServletRequest) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long userId=loginUser.getId();
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        // 参数校验
        Throw.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        // 调用Service
        Page<ContactVO> pendingPage = contactService.getPendingRequests(request, userId);
        return Result.success(pendingPage);
    }

    /**
     * 删除联系人
     */
    @PostMapping("/delete")
    @Operation(summary = "删除联系人")
    public BaseResponse<Boolean> deleteContact(@RequestBody DeleteRequest deleteRequest, HttpServletRequest httpServletRequest) {
        // 获取当前登录用户ID
        User loginUser = userService.getLoginUser(httpServletRequest);
        Long userId=loginUser.getId();
        Throw.throwIf(userId == null, ErrorCode.NOT_LOGIN_ERROR);
        // 参数校验
        Throw.throwIf(deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 调用Service
        boolean result = contactService.removeContact(deleteRequest.getId(), userId);
        return Result.success(result);
    }
}
