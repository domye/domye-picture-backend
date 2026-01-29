package com.domye.picture.api.controller;

import com.domye.picture.core.result.BaseResponse;
import com.domye.picture.core.result.Result;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.dto.command.user.UserLoginCommand;
import com.domye.picture.service.dto.command.user.UserRegisterCommand;
import com.domye.picture.service.dto.presentation.user.LoginUserVO;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

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
     * @param request          http请求
     * @return 脱敏后的登录用户信息
     */
    @ApiOperation(value = "用户登录")
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginCommand command, HttpServletRequest request) {

        return Result.success(userService.userLogin(command, request));
    }

    /**
     * 获取当前登录用户信息
     * @param request http请求
     * @return 脱敏后的登录用户信息
     */
    @ApiOperation(value = "获取当前登录用户信息")
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
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


}