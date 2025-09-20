package com.domye.picture.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum SpaceTypeEnum {
    PRIVATE("私用空间", 0),
    TEAM("团队空间", 1);
    private final String name;
    private final int value;

    SpaceTypeEnum(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static SpaceTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (SpaceTypeEnum spaceTypeEnum : SpaceTypeEnum.values()) {
            if (spaceTypeEnum.value == value) {
                return spaceTypeEnum;
            }
        }
        return null;
    }
}
