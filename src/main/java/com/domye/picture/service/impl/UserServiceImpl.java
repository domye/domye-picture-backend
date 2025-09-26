/*******************    💫 Codegeex Inline Diff    *******************/
package com.domye.picture.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.exception.BusinessException;
import com.domye.picture.exception.ErrorCode;
import com.domye.picture.manager.auth.StpKit;
import com.domye.picture.mapper.UserMapper;
import com.domye.picture.model.dto.user.UserQueryRequest;
import com.domye.picture.model.entity.User;
import com.domye.picture.model.enums.UserRoleEnum;
import com.domye.picture.model.vo.LoginUserVO;
import com.domye.picture.model.vo.UserVO;
import com.domye.picture.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.domye.picture.constant.UserConstant.USER_LOGIN_STATE;

/**
 * @author Domye
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-08-26 15:00:43
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    /**
     * 用户注册
     * @param userAccount   用户账户
     * @param password      用户密码
     * @param checkPassword 确认密码
     * @return Long 用户id
     */
    @Override
    public Long UserRegister(String userAccount, String password, String checkPassword) {
        //1. 检验参数是否合法
        if (StrUtil.hasBlank(userAccount, password, checkPassword))
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        if (userAccount.length() < 6 || userAccount.length() > 16)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度必须在6-16位之间");
        if (password.length() < 6 || password.length() > 16)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须在6-16位之间");
        if (!password.equals(checkPassword))
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");

        //2.数据库检测账号是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        //3.加密数据
        String encryptPassword = getEncryptPassword(password);
        //4.将加密的数据存入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        //5.返回用户id
        return user.getId();

    }

    /**
     * 用户登录
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @param request      HTTP请求对象
     * @return LoginUserVO 登陆的用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //校验
        if (StrUtil.hasBlank(userAccount, userPassword))
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数不能为空");
        if (userAccount.length() < 6 || userAccount.length() > 16)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度必须在6-16位之间");
        if (userPassword.length() < 6 || userPassword.length() > 16)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度必须在6-16位之间");
        //用户传递密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        //查询数据是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount).eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        //不存在抛出异常
        if (user == null)
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户不存在或密码错误");
        //正确则返回用户信息
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取视图层的登录用户信息
     * @param user
     * @param user 用户实体对象
     * @return LoginUserVO 登录用户的视图对象
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return User 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 退出登录
     * @param request
     * @return boolean 是否成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }

        User currentUser = (User) userObj;
        // Sa-Token 登出
        StpKit.SPACE.logout(currentUser.getId());

        // 移除 Session 登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);

        return true;
    }

    /**
     * 获取视图层用户信息
     * @param user
     * @return UserVO 用户视图对象
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取视图层用户信息列表
     * @param userList 用户列表
     * @return List<UserVO> 用户视图对象列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 构造查询条件
     * @param userQueryRequest 用户查询请求
     * @return QueryWrapper<User> 查询条件
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取加密后的密码
     * @param userPassword 用户密码
     * @return String 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "domye";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 判断当前用户是否为管理员
     * @param user
     * @return boolean 是否为管理员
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

}




