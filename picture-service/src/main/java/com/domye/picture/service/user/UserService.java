package com.domye.picture.service.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.domye.picture.model.dto.user.UserQueryRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.vo.user.LoginUserVO;
import com.domye.picture.model.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Domye
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-08-26 15:00:43
 */
public interface UserService extends IService<User> {
    Long UserRegister(String userAccount, String password, String checkPassword);

    /**
     * 用户登录
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取脱敏后的用户信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    User getLoginUser(HttpServletRequest request);

    boolean userLogout(HttpServletRequest request);

    public UserVO getUserVO(User user);

    public List<UserVO> getUserVOList(List<User> userList);

    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    public String getEncryptPassword(String defaultPassword);

    public boolean isAdmin(User user);

    /**
     * 根据openId查找用户
     * @param openId 微信openId
     * @return 用户对象
     */
    User findByOpenId(String openId);

    /**
     * 创建微信用户
     * @param openId 微信openId
     * @return 用户对象
     */
    User createWxUser(String openId);

    /**
     * 微信登录
     * @param user    用户对象
     * @param request HTTP请求
     * @return 登录用户信息
     */
    void wxLogin(User user, HttpServletRequest request);

    void loginByWx(String fromUserName, HttpServletRequest request);

    void bindWx(String fromUserName, Long userId);
}

