package com.domye.picture.model.enums;

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

}
