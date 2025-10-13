package com.domye.picture.service.user.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户黑名单枚举
 */
@Getter
public enum FilterTypeEnum {

    PICTURE_UPLOAD("picture_upload", 0L),
    PICTURE_LOGIN("user_login", 1L),
    ;

    private final String text;

    private final Long value;

    FilterTypeEnum(String text, Long value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static String getTextByValue(Long value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (FilterTypeEnum userRoleEnum : FilterTypeEnum.values()) {
            if (userRoleEnum.value == value) {
                return userRoleEnum.text;
            }
        }
        return null;
    }
}
