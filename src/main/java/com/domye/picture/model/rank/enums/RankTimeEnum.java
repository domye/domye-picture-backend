package com.domye.picture.model.rank.enums;

import lombok.Getter;

@Getter
public enum RankTimeEnum {
    DAY("day", 1),
    MONTH("month", 2),
    ;

    private String name;
    private int value;

    RankTimeEnum(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static boolean isDay(int value) {
        return RankTimeEnum.DAY.getValue() == value;
    }

}
