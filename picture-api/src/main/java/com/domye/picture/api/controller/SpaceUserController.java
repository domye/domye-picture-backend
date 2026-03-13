package com.domye.picture.api.controller;

import com.domye.picture.auth.annotation.SaSpaceCheckPermission;
import com.domye.picture.auth.model.SpaceUserPermissionConstant;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.DeleteRequest;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.space.SpaceUserAddRequest;
import com.domye.picture.model.dto.space.SpaceUserEditRequest;
import com.domye.picture.model.dto.space.SpaceUserQueryRequest;
import com.domye.picture.model.entity.space.SpaceUser;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.space.SpaceUserVO;
import com.domye.picture.service.api.space.SpaceUserService;
import com.domye.picture.service.api.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * 添加成员到空间
     */
    @Operation(summary = "添加成员到空间")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest, HttpServletRequest request) {
        Throw.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long id = spaceUserService.addSpaceUser(spaceUserAddRequest, loginUser.getId());
        return Result.success(id);
    }

    /**
     * 从空间移除成员
     */
    @Operation(summary = "从空间移除成员")
    @PostMapping("/delete")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> deleteSpaceUser(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        Throw.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        spaceUserService.deleteSpaceUser(deleteRequest.getId(), loginUser.getId());
        return Result.success(true);
    }

    /**
     * 查询某个成员在某个空间的信息
     */
    @Operation(summary = "查询某个成员在某个空间的信息")
    @PostMapping("/get")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<SpaceUser> getSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        SpaceUser spaceUser = spaceUserService.getSpaceUser(spaceUserQueryRequest);
        return Result.success(spaceUser);
    }

    /**
     * 查询成员信息列表
     */
    @Operation(summary = "查询成员信息列表")
    @PostMapping("/list")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<List<SpaceUserVO>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        List<SpaceUserVO> spaceUserVOList = spaceUserService.listSpaceUser(spaceUserQueryRequest);
        return Result.success(spaceUserVOList);
    }

    /**
     * 编辑成员信息（设置权限）
     */
    @Operation(summary = "编辑成员信息（设置权限）")
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest, HttpServletRequest request) {
        Throw.throwIf(spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        spaceUserService.editSpaceUser(spaceUserEditRequest, loginUser.getId());
        return Result.success(true);
    }

    /**
     * 查询我加入的团队空间列表
     */
    @Operation(summary = "查询我加入的团队空间列表")
    @PostMapping("/list/my")
    public BaseResponse<List<SpaceUserVO>> listMyTeamSpace(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<SpaceUserVO> spaceUserVOList = spaceUserService.listMyTeamSpace(loginUser.getId());
        return Result.success(spaceUserVOList);
    }
}