package com.domye.picture.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.common.BaseResponse;
import com.domye.picture.common.DeleteRequest;
import com.domye.picture.common.Result;
import com.domye.picture.constant.UserConstant;
import com.domye.picture.exception.BusinessException;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.exception.Throw;
import com.domye.picture.manager.auth.SpaceUserAuthManager;
import com.domye.picture.service.space.SpaceService;
import com.domye.picture.service.space.model.dto.SpaceAddRequest;
import com.domye.picture.service.space.model.dto.SpaceEditRequest;
import com.domye.picture.service.space.model.dto.SpaceQueryRequest;
import com.domye.picture.service.space.model.dto.SpaceUpdateRequest;
import com.domye.picture.service.space.model.entity.Space;
import com.domye.picture.service.space.model.entity.SpaceLevel;
import com.domye.picture.service.space.model.enums.SpaceLevelEnum;
import com.domye.picture.service.space.model.vo.SpaceVO;
import com.domye.picture.service.user.UserService;
import com.domye.picture.service.user.model.entity.User;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/space")
public class SpaceController {
    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 创建空间
     * @param spaceAddRequest 空间上传请求
     * @param request         http请求
     * @return 空间id
     */
    @ApiOperation("创建空间")
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(
            SpaceAddRequest spaceAddRequest,
            HttpServletRequest request) {
        Throw.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Long spaceId = spaceService.addSpace(spaceAddRequest, loginUser);
        return Result.success(spaceId);
    }

    /**
     * 删除空间
     * @param deleteRequest 删除请求
     * @param request       http请求
     * @return 删除是否成功
     */
    @ApiOperation("删除空间")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        spaceService.deleteSpace(id, loginUser);
        return Result.success(true);
    }

    /**
     * 更新空间(管理员)
     * @param spaceUpdateRequest
     * @param request
     * @return 是否更新成功
     */
    @ApiOperation("更新空间")
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 数据校验
        spaceService.fillSpace(space);
        spaceService.validSpace(space, false);
        // 判断是否存在
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        Throw.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        boolean result = spaceService.updateById(space);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return Result.success(true);
    }


    /**
     * 根据id获取空间(管理员)
     * @param id      空间id
     * @param request http请求
     * @return 空间信息
     */
    @ApiOperation("根据id获取空间")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return Result.success(space);
    }

    /**
     * 根据id获取空间封装类
     * @param id      空间id
     * @param request http请求
     * @return 脱敏后的空间信息
     */
    @ApiOperation("根据id获取空间封装类")
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        Throw.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        spaceVO.setPermissionList(permissionList);
        // 获取封装类
        return Result.success(spaceVO);
    }

    /**
     * 分页获取空间列表(管理员)
     * @param spaceQueryRequest 查询请求
     * @return 空间分页信息
     */
    @ApiOperation("分页获取空间列表")
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return Result.success(spacePage);
    }

    /**
     * 分页获取空间封装类列表
     * @param spaceQueryRequest 查询请求
     * @param request           http请求
     * @return 脱敏后的空间分页信息
     */
    @ApiOperation("分页获取空间封装类列表")
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                         HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        Throw.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        // 获取封装类
        return Result.success(spaceService.getSpaceVOPage(spacePage, request));
    }

    /**
     * 编辑空间
     * @param spaceEditRequest 编辑请求
     * @param request          http请求
     * @return 是否编辑成功
     */
    @ApiOperation("编辑空间")
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        // 自动填充数据
        spaceService.fillSpace(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 数据校验
        spaceService.validSpace(space, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
        Throw.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = spaceService.updateById(space);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return Result.success(true);
    }


    /**
     * 获取空间权限列表
     * @return 空间权限列表
     */
    @ApiOperation("获取空间权限列表")
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return Result.success(spaceLevelList);
    }
}
