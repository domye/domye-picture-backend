package com.domye.picture.model.vo.user;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户主页信息 VO
 */
@Data
public class UserProfileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    private String userAccount;
    /**
     * 用户昵称
     */
    private String userName;
    /**
     * 用户头像
     */
    private String userAvatar;
    /**
     * 用户简介
     */
    private String userProfile;
    /**
     * 用户角色：user/admin
     */
    private String userRole;


}
