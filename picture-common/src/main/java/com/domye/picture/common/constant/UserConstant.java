package com.domye.picture.common.constant;

public interface UserConstant {
    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    // region 账号密码校验

    /**
     * 账号最小长度
     */
    int ACCOUNT_MIN_LENGTH = 6;

    /**
     * 账号最大长度
     */
    int ACCOUNT_MAX_LENGTH = 16;

    /**
     * 密码最小长度
     */
    int PASSWORD_MIN_LENGTH = 6;

    /**
     * 密码最大长度
     */
    int PASSWORD_MAX_LENGTH = 16;

    // endregion

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

}