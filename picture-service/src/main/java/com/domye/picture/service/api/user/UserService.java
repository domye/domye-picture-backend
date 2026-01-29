package com.domye.picture.service.api.user;

import com.domye.picture.service.dto.command.user.UserLoginCommand;
import com.domye.picture.service.dto.command.user.UserRegisterCommand;
import com.domye.picture.service.dto.presentation.user.LoginUserVO;

import javax.servlet.http.HttpServletRequest;

public interface UserService {
    long UserRegister(UserRegisterCommand command);

    LoginUserVO userLogin(UserLoginCommand command,HttpServletRequest request);

    LoginUserVO getLoginUser(HttpServletRequest request);

    Boolean userLogout(HttpServletRequest request);
}
