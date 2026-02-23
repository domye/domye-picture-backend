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

    // endregion

    // region 错误消息

    /**
     * 账号已存在
     */
    String ACCOUNT_ALREADY_EXISTS = "账号重复";

    /**
     * 密码错误
     */
    String PASSWORD_ERROR = "用户不存在或密码错误";

    /**
     * 用户未找到
     */
    String USER_NOT_FOUND = "用户不存在或密码错误";

    /**
     * 两次密码不一致
     */
    String PASSWORD_MISMATCH = "两次输入密码不一致";

    /**
     * 默认用户名
     */
    String DEFAULT_USER_NAME = "无名";

    /**
     * 默认微信用户名
     */
    String DEFAULT_WX_USER_NAME = "微信用户";

    // endregion
}
