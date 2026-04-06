package com.domye.picture.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

@Getter
public enum AlbumTypeEnum {
    Not("不属于", 0),
    Own("属于", 1),
    Cover("主图", 2);

    private final String name;
    private final int value;

    AlbumTypeEnum(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static AlbumTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AlbumTypeEnum albumTypeEnum : AlbumTypeEnum.values()) {
            if (albumTypeEnum.value == value) {
                return albumTypeEnum;
            }
        }
        return null;
    }
}
