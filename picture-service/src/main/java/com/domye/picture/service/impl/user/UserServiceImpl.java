package com.domye.picture.service.impl.user;

import cn.hutool.core.util.StrUtil;
import com.domye.picture.core.exception.ErrorCode;
import com.domye.picture.core.exception.Throw;
import com.domye.picture.core.model.user.User;
import com.domye.picture.repository.user.UserRepository;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.converter.user.UserConverter;
import com.domye.picture.service.dto.command.user.UserLoginCommand;
import com.domye.picture.service.dto.command.user.UserRegisterCommand;
import com.domye.picture.service.dto.presentation.user.LoginUserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;

import static com.domye.picture.common.constant.UserConstant.USER_LOGIN_STATE;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserConverter userConverter;
    private final UserRepository userRepository;

    @Override
    public long UserRegister(UserRegisterCommand command) {
        Throw.throwIf(command == null, ErrorCode.PARAMS_ERROR);
        String userAccount = command.getUserAccount();
        String password = command.getUserPassword();
        String checkPassword = command.getCheckPassword();

        //1. 检验参数是否合法
        Throw.throwIf(StrUtil.hasBlank(userAccount, password, checkPassword), ErrorCode.PARAMS_ERROR, "参数不能为空");
        Throw.throwIf(userAccount.length() < 6 || userAccount.length() > 16, ErrorCode.PARAMS_ERROR, "账号长度必须在6-16位之间");
        Throw.throwIf(password.length() < 6 || password.length() > 16, ErrorCode.PARAMS_ERROR, "密码长度必须在6-16位之间");
        Throw.throwIf(!password.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入密码不一致");

        //2.数据库检测账号是否存在
        long count = userRepository.getUserCount(userAccount);
        Throw.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复");

        User user = userConverter.fromCommand(command);
        user.setUserName("无名");
        boolean saveResult = userRepository.save(user);
        Throw.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败");


        //5.返回用户id
        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(UserLoginCommand command, HttpServletRequest request) {
        Throw.throwIf(command == null, ErrorCode.PARAMS_ERROR);
        String userAccount = command.getUserAccount();
        String userPassword = command.getUserPassword();
        //校验
        Throw.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR, "参数不能为空");
        Throw.throwIf(userAccount.length() < 6 || userAccount.length() > 16, ErrorCode.PARAMS_ERROR, "账号长度必须在6-16位之间");
        Throw.throwIf(userPassword.length() < 6 || userPassword.length() > 16, ErrorCode.PARAMS_ERROR, "密码长度必须在6-16位之间");

        //用户传递密码加密
        String encryptPassword = getEncryptPassword(userPassword);

        User user=userRepository.getUser(userAccount,encryptPassword);
        Throw.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户不存在或密码错误");
        //正确则返回用户信息
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        //TODO SATOKEN
//        StpKit.SPACE.login(user.getId());
//        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        return userConverter.toVO(user);
    }

    @Override
    public LoginUserVO getLoginUser(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        Throw.throwIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        return  userConverter.toVO(currentUser);
    }

    @Override
    public Boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        Throw.throwIf(userObj == null, ErrorCode.OPERATION_ERROR, "未登录");

        User currentUser = (User) userObj;
        // TODO Sa-Token 登出
//        StpKit.SPACE.logout(currentUser.getId());

        // 移除 Session 登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "domye";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
}
