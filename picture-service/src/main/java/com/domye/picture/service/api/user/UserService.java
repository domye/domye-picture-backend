package com.domye.picture.service.api.user;

import com.domye.picture.core.model.user.User;
import com.domye.picture.service.dto.command.user.UserLoginCommand;
import com.domye.picture.service.dto.command.user.UserRegisterCommand;
import com.domye.picture.service.dto.presentation.user.UserVO;

import javax.servlet.http.HttpServletRequest;

public interface UserService{
    long UserRegister(UserRegisterCommand command);

    UserVO userLogin(UserLoginCommand command, HttpServletRequest request);

    UserVO getLoginUser(HttpServletRequest request);

    Boolean userLogout(HttpServletRequest request);

    User getById(long id);

    UserVO getUserVOById(long id);

    Boolean removeById(Long id);
}
