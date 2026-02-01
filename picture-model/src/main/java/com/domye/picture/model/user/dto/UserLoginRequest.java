package com.domye.picture.model.user.dto;

import lombok.Data;

@Data
public class UserLoginRequest {
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
