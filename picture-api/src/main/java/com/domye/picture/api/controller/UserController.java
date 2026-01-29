package com.domye.picture.api.controller;

import com.domye.picture.core.exception.ErrorCode;
import com.domye.picture.core.exception.Throw;
import com.domye.picture.core.model.user.User;
import com.domye.picture.core.result.BaseResponse;
import com.domye.picture.core.result.DeleteRequest;
import com.domye.picture.core.result.Result;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.dto.command.user.UserLoginCommand;
import com.domye.picture.service.dto.command.user.UserRegisterCommand;
import com.domye.picture.service.dto.command.user.UserUpdateCommand;
import com.domye.picture.service.dto.presentation.user.UserVO;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Objects;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController implements Serializable {

    private final UserService userService;

    /**
     * 用户注册
     * @param command 用户注册请求
     * @return 用户id
     */
    @ApiOperation(value = "用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterCommand command) {
        return Result.success(userService.UserRegister(command));
    }

    /**
     * 用户登录
     * @param command 用户登录请求
     * @param request http请求
     * @return 脱敏后的登录用户信息
     */
    @ApiOperation(value = "用户登录")
    @PostMapping("/login")
    public BaseResponse<UserVO> userLogin(@RequestBody UserLoginCommand command, HttpServletRequest request) {

        return Result.success(userService.userLogin(command, request));
    }

    /**
     * 获取当前登录用户信息
     * @param request http请求
     * @return 脱敏后的登录用户信息
     */
    @ApiOperation(value = "获取当前登录用户信息")
    @GetMapping("/get/login")
    public BaseResponse<UserVO> getLoginUser(HttpServletRequest request) {
        return Result.success(userService.getLoginUser(request));
    }

    /**
     * 用户注销
     * @param request http请求
     * @return 是否退出成功
     */
    @ApiOperation(value = "用户注销")
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        return Result.success(userService.userLogout(request));
    }

    /**
     * 根据 id 获取用户
     * @param id 用户id
     * @return 用户信息
     */
    @ApiOperation(value = "根据 id 获取用户")
    @GetMapping("/get")
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
        return Result.success(userService.getUserVOById(id));
    }

    /**
     * 删除用户
     * @param deleteRequest 删除请求
     * @return 用户列表
     */
    @ApiOperation(value = "删除用户")
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        Throw.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        return Result.success(userService.removeById(deleteRequest.getId()));
    }


}