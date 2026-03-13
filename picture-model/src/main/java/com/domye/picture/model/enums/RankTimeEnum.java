package com.domye.picture.model.enums;

import lombok.Getter;

/**
 * 排行榜时间维度枚举
 */
@Getter
public enum RankTimeEnum {
    /**
     * 日榜
     */
    DAY("day", 1),

    /**
     * 周榜
     */
    WEEK("week", 2),

    /**
     * 月榜
     */
    MONTH("month", 3),

    /**
     * 总榜
     */
    TOTAL("total", 4),
    ;

    private final String name;
    private final int value;

    RankTimeEnum(String name, int value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static RankTimeEnum getEnumByValue(int value) {
        for (RankTimeEnum rankTimeEnum : RankTimeEnum.values()) {
            if (rankTimeEnum.getValue() == value) {
                return rankTimeEnum;
            }
        }
        return null;
    }

}
