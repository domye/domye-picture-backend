package com.domye.picture.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 投票活动状态枚举
 */
@Getter
public enum VoteActivitiesStatusEnum {

    NOT_STARTED("未开始", 0),
    IN_PROGRESS("进行中", 1),
    FINISHED("已结束", 2),
    ;

    private final String text;
    private final Integer value;

    VoteActivitiesStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static VoteActivitiesStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (VoteActivitiesStatusEnum statusEnum : VoteActivitiesStatusEnum.values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
