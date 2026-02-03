package com.domye.picture.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户黑名单枚举
 */
@Getter
public enum FilterModeEnum {

    BLACK("black_list", 0L),
    WHITE("white_list", 1L);

    private final String text;

    private final Long value;

    FilterModeEnum(String text, Long value) {
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
        for (FilterModeEnum userRoleEnum : FilterModeEnum.values()) {
            if (userRoleEnum.value == value) {
                return userRoleEnum.text;
            }
        }
        return null;
    }
}
