package com.domye.picture.api.controller;

import com.domye.picture.core.exception.ErrorCode;
import com.domye.picture.core.exception.Throw;
import com.domye.picture.core.result.BaseResponse;
import com.domye.picture.core.result.Result;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.dto.command.user.UserLoginCommand;
import com.domye.picture.service.dto.command.user.UserRegisterCommand;
import com.domye.picture.service.dto.presentation.user.LoginUserVO;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController implements Serializable {

    private final UserService userService;

    /**
     * 用户注册
     * @param UserRegisterCommand 用户注册请求
     * @return 用户id
     */
    @ApiOperation(value = "用户注册")
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterCommand command) {
        long result = userService.UserRegister(command);
        return Result.success(result);
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
        Throw.throwIf(command == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userService.userLogin(command, request);
        return Result.success(loginUserVO);
    }

}