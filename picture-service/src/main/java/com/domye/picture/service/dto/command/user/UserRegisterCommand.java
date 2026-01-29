package com.domye.picture.service.dto.command.user;

import lombok.Data;

@Data
public class UserRegisterCommand {

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
