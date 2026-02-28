package com.domye.picture.service.api.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.user.UserQueryRequest;
import com.domye.picture.model.dto.user.UserUpdateRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.user.UserVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * @author Domye
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-08-26 15:00:43
 */
public interface UserService extends IService<User> {
    Long UserRegister(String userAccount, String password, String checkPassword);

    UserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    String getEncryptPassword(String defaultPassword);

    boolean isAdmin(User user);


    void validateAccountAndPassword(String userAccount, String password);

    boolean updateUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);
}

