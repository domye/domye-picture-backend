package com.domye.picture.model.enums;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

@Getter
public enum UserRoleEnum {
    USER("用户", "user"),
    ADMIN("管理员", "admin");

    private final String text;
    private final String value;

    /**
     * 构造方法
     * @param text
     * @param value
     */
    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public static UserRoleEnum getByValue(String value) {
        if (ObjectUtil.isEmpty(value))
            return null;
        for (UserRoleEnum e : UserRoleEnum.values()) {
            if (e.getValue().equals(value)) {
                return e;
            }
        }
        return null;
    }
}
