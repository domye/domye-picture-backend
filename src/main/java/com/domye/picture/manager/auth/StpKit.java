package com.domye.picture.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import org.springframework.stereotype.Component;

/**
 * StpLogic 门面类，管理项目中所有的 StpLogic 账号体系
 * 添加 @Component 注解的目的是确保静态属性 DEFAULT 和 SPACE 被初始化
 */
@Component
public class StpKit {

    public static final String SPACE_TYPE = "space";


    /**
     * Space 会话对象，管理 Space 表所有账号的登录、权限认证
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);
}
