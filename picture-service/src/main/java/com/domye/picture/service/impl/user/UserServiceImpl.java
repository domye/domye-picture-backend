package com.domye.picture.service.impl.user;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.domye.picture.common.auth.StpKit;
import com.domye.picture.common.exception.ErrorCode;
import com.domye.picture.common.exception.Throw;
import com.domye.picture.model.dto.user.UserQueryRequest;
import com.domye.picture.model.dto.user.UserUpdateRequest;
import com.domye.picture.model.entity.user.User;
import com.domye.picture.model.enums.UserRoleEnum;
import com.domye.picture.model.mapper.user.UserStructMapper;
import com.domye.picture.model.vo.user.UserVO;
import com.domye.picture.service.api.user.UserService;
import com.domye.picture.service.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.domye.picture.common.constant.UserConstant.*;

/**
 * @author Domye
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-08-26 15:00:43
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    private final UserStructMapper userStructMapper;

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param password      用户密码
     * @param checkPassword 确认密码
     * @return Long 用户id
     */
    @Override
    public Long UserRegister(String userAccount, String password, String checkPassword) {
        //1. 检验参数是否合法
        validateAccountAndPassword(userAccount, password);
        Throw.throwIf(!password.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入密码不一致");

        //2.数据库检测账号是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        long count = baseMapper.selectCount(queryWrapper);
        Throw.throwIf(count > 0, ErrorCode.PARAMS_ERROR, "账号重复");

        //3.加密数据
        String encryptPassword = getEncryptPassword(password);

        //4.将加密的数据存入数据库
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userName("无名")
                .userRole(UserRoleEnum.USER.getValue())
                .build();
        boolean saveResult = save(user);
        Throw.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "注册失败");
        //5.返回用户id
        return user.getId();

    }

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @param request      HTTP请求对象
     * @return LoginUserVO 登陆的用户信息
     */
    @Override
    public UserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        //校验
        validateAccountAndPassword(userAccount, userPassword);

        //用户传递密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        //查询数据是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount).eq("userPassword", encryptPassword);
        User user = baseMapper.selectOne(queryWrapper);
        Throw.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR, "用户不存在或密码错误");
        //正确返回用户信息
        setLoginState(user, request);
        return userStructMapper.toUserVo(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return User 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        Throw.throwIf(currentUser == null || currentUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);
        return currentUser;
    }

    /**
     * 退出登录
     *
     * @param request
     * @return boolean 是否成功
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        Throw.throwIf(userObj == null, ErrorCode.OPERATION_ERROR, "未登录");

        User currentUser = (User) userObj;
        // Sa-Token 登出
        StpKit.SPACE.logout(currentUser.getId());

        // 移除 Session 登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }


    /**
     * 构造查询条件
     *
     * @param userQueryRequest 用户查询请求
     * @return QueryWrapper<User> 查询条件
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        Throw.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        Long id = userQueryRequest.getId();

        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();

        QueryWrapper<User> queryWrapper = new QueryWrapper<User>()
                .eq(ObjUtil.isNotNull(id), "id", id)
                .eq(StrUtil.isNotBlank(userRole), "userRole", userRole)
                .like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount)
                .like(StrUtil.isNotBlank(userName), "userName", userName)
                .like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile)
                .orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 获取加密后的密码
     *
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
     *
     * @param user
     * @return boolean 是否为管理员
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 校验用户账号和密码参数
     *
     * @param userAccount 用户账号
     * @param password    用户密码
     */
    @Override
    public void validateAccountAndPassword(String userAccount, String password) {
        Throw.throwIf(StrUtil.hasBlank(userAccount, password), ErrorCode.PARAMS_ERROR, "参数不能为空");
        Throw.throwIf(userAccount.length() < ACCOUNT_MIN_LENGTH || userAccount.length() > ACCOUNT_MAX_LENGTH,
                ErrorCode.PARAMS_ERROR, "账号长度必须在" + ACCOUNT_MIN_LENGTH + "-" + ACCOUNT_MAX_LENGTH + "位之间");
        Throw.throwIf(password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH,
                ErrorCode.PARAMS_ERROR, "密码长度必须在" + PASSWORD_MIN_LENGTH + "-" + PASSWORD_MAX_LENGTH + "位之间");
    }

    /**
     * 设置用户登录状态
     *
     * @param user    用户对象
     * @param request HTTP请求
     */
    private void setLoginState(User user, HttpServletRequest request) {
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        StpKit.SPACE.login(user.getId());
        StpKit.SPACE.getSession().set(USER_LOGIN_STATE, user);
    }

    /**
     * 更新用户信息
     *
     * @param userUpdateRequest 用户更新请求
     * @param request           http请求
     * @return 是否更新成功
     */
    @Override
    public boolean updateUser(UserUpdateRequest userUpdateRequest, HttpServletRequest request) {
        Throw.throwIf(userUpdateRequest == null || userUpdateRequest.getId() == null, ErrorCode.PARAMS_ERROR);

        User loginUser = getLoginUser(request);
        User oldUser = getById(userUpdateRequest.getId());
        Throw.throwIf(oldUser == null, ErrorCode.NOT_FOUND_ERROR);

        User user = userStructMapper.toUser(userUpdateRequest);
        Throw.throwIf(!Objects.equals(user.getId(), loginUser.getId()) && !isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        Throw.throwIf(!oldUser.getUserRole().equals(user.getUserRole()) && !isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);

        boolean result = updateById(user);
        Throw.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    /**
     * 分页获取用户封装列表
     *
     * @param userQueryRequest 用户查询请求
     * @return 用户封装列表
     */
    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        Throw.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = page(new Page<>(current, pageSize), getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userPage.getRecords().stream()
                .map(userStructMapper::toUserVo)
                .collect(Collectors.toList());
        userVOPage.setRecords(userVOList);
        return userVOPage;
    }
}




