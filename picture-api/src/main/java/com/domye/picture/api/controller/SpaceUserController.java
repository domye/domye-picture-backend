package com.domye.picture.api.controller;

import cn.hutool.core.util.ObjectUtil;
import com.domye.picture.auth.annotation.SaSpaceCheckPermission;
import com.domye.picture.auth.model.SpaceUserPermissionConstant;
import com.domye.picture.common.auth.StpKit;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.DeleteRequest;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.space.SpaceUserAddRequest;
import com.domye.picture.model.dto.space.SpaceUserEditRequest;
import com.domye.picture.model.dto.space.SpaceUserQueryRequest;
import com.domye.picture.model.entity.contact.Contact;
import com.domye.picture.model.entity.space.SpaceUser;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.ContactStatusEnum;
import com.domye.picture.model.vo.space.SpaceUserVO;
import com.domye.picture.service.api.contact.ContactService;
import com.domye.picture.service.api.space.SpaceUserService;
import com.domye.picture.service.api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间成员管理
 */
@RestController
@RequestMapping("/spaceUser")
@Slf4j
@RequiredArgsConstructor
public class SpaceUserController {

   final SpaceUserService spaceUserService;

   final UserService userService;

   final ContactService contactService;

    /**
     * 添加成员到空间
     */
    @Operation(summary = "添加成员到空间")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.SPACE_USER_MANAGE);
        Throw.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        Throw.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取当前登录用户ID
        Long loginUserId = Long.valueOf(StpKit.SPACE.getLoginIdDefaultNull().toString());
        Long spaceId = spaceUserAddRequest.getSpaceId();
        Long targetUserId = spaceUserAddRequest.getUserId();

        // 检查当前用户是否是该空间的管理员
        SpaceUserQueryRequest queryRequest = new SpaceUserQueryRequest();
        queryRequest.setSpaceId(spaceId);
        queryRequest.setUserId(loginUserId);
        SpaceUser currentUserSpaceRole = spaceUserService.getOne(spaceUserService.getQueryWrapper(queryRequest));

        // 如果不是管理员，需要检查是否在联系人列表中
        if (currentUserSpaceRole == null || !"admin".equals(currentUserSpaceRole.getSpaceRole())) {
            // 查询联系人表，检查是否存在已通过的联系关系
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Contact> contactQuery =
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            contactQuery.eq("user_id", loginUserId)
                    .eq("contact_user_id", targetUserId)
                    .eq("status", ContactStatusEnum.ACCEPTED.getValue());
            Contact contact = contactService.getOne(contactQuery);
            Throw.throwIf(contact == null, ErrorCode.NO_AUTH_ERROR, "非管理员只能从联系人中添加成员");
        }

        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return Result.success(id);
    }

    /**
     * 从空间移除成员
     */
    @Operation(summary = "从空间移除成员")
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest,
                                                 HttpServletRequest request) {
        boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.SPACE_USER_MANAGE);
        Throw.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        Throw.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        long id = deleteRequest.getId();
        // 判断是否存在
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        Throw.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        isMyself(request, oldSpaceUser);
        // 操作数据库
        boolean result = spaceUserService.removeById(id);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return Result.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @Operation(summary = "查询某个成员在某个空间的信息")
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        // 参数校验
        Throw.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        Throw.throwIf(ObjectUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        Throw.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        return Result.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @Operation(summary = "查询成员信息列表")
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest,
                                                         HttpServletRequest request) {
        Throw.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return Result.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @Operation(summary = "编辑成员信息（设置权限）")
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest,
                                               HttpServletRequest request) {
        //鉴权
        boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.SPACE_USER_MANAGE);
        Throw.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        Throw.throwIf(spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        spaceUserService.validSpaceUser(spaceUser, false);
        // 判断是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        Throw.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        isMyself(request, oldSpaceUser);
        // 操作数据库
        boolean result = spaceUserService.updateById(spaceUser);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return Result.success(true);
    }


    private void isMyself(HttpServletRequest request, SpaceUser oldSpaceUser) {
        long loginUserId = userService.getLoginUser(request).getId();
        Throw.throwIf(oldSpaceUser.getUserId() == loginUserId, ErrorCode.NO_AUTH_ERROR, "无权限编辑自己");

    }

    /**
     * 查询我加入的团队空间列表
     */
    @Operation(summary = "查询我加入的团队空间列表")
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return Result.success(spaceUserService.getSpaceUserVOList(spaceUserList));
    }
}