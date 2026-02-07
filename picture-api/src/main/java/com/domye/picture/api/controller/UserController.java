package com.domye.picture.api.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.domye.picture.auth.annotation.AuthCheck;
import com.domye.picture.common.constant.UserConstant;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.common.result.BaseResponse;
import com.domye.picture.common.result.DeleteRequest;
import com.domye.picture.common.result.Result;
import com.domye.picture.model.dto.user.UserLoginRequest;
import com.domye.picture.model.dto.user.UserQueryRequest;
import com.domye.picture.model.dto.user.UserRegisterRequest;
import com.domye.picture.model.dto.user.UserUpdateRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.user.LoginUserVO;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.api.user.UserService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController implements Serializable {
    final UserService userService;

    /**
     * 用户注册
     * @param userRegisterRequest 用户注册请求
     * @return 用户id
     */
    @ApiOperation(value = "用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        Throw.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.UserRegister(userAccount, userPassword, checkPassword);
        return Result.success(result);
    }

    /**
     * 用户登录
     * @param userLoginRequest 用户登录请求
     * @param request          http请求
     * @return 脱敏后的登录用户信息
     */
    @ApiOperation(value = "用户登录")
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        Throw.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return Result.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息
     * @param request http请求
     * @return 脱敏后的登录用户信息
     */
    @ApiOperation(value = "获取当前登录用户信息")
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return Result.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     * @param request http请求
     * @return 是否退出成功
     */
    @ApiOperation(value = "用户注销")
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        Throw.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return Result.success(result);
    }


    /**
     * 根据 id 获取用户
     * @param id 用户id
     * @return 用户信息
     */
    @ApiOperation(value = "根据 id 获取用户")
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        Throw.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        Throw.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return Result.success(user);
    }

    /**
     * 根据 id 获取用户封装信息
     * @param id 用户id
     * @return 脱敏后的用户信息
     */
    @ApiOperation(value = "根据 id 获取用户封装信息")
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return Result.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     * @param deleteRequest 删除请求
     * @return 用户列表
     */
    @ApiOperation(value = "删除用户")
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        Throw.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        boolean b = userService.removeById(deleteRequest.getId());
        return Result.success(b);
    }

    /**
     * 更新用户信息
     * @param userUpdateRequest 用户更新请求
     * @param request           http请求
     * @return 是否更新成功
     */
    @ApiOperation(value = "更新用户信息")
    @PostMapping("/update")
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        Throw.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        User oldUser = userService.getById(userUpdateRequest.getId());
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        Throw.throwIf(!Objects.equals(user.getId(), loginUser.getId()) || !userService.isAdmin(user), ErrorCode.NO_AUTH_ERROR);
        Throw.throwIf(!oldUser.getUserRole().equals(user.getUserRole()) && !userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);


        boolean result = userService.updateById(user);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return Result.success(true);
    }

    /**
     * 分页获取用户封装列表
     * @param userQueryRequest 用户查询请求
     * @return 用户封装列表
     */
    @ApiOperation(value = "分页获取用户封装列表")
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        Throw.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return Result.success(userVOPage);
    }


}
