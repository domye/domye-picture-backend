package com.domye.picture.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.io.Serializable;

/**
 * 信息流类型枚举
 */
@Getter
public enum FeedTypeEnum implements Serializable {

    FOLLOW("关注流", 0),
    RECOMMEND("推荐流", 1),
    LATEST("最新流", 2);

    private static final long serialVersionUID = 1L;

    private final String name;
    private final Integer value;

    FeedTypeEnum(String name, Integer value) {
        this.name = name;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return 枚举实例
     */
    public static FeedTypeEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (FeedTypeEnum feedTypeEnum : FeedTypeEnum.values()) {
            if (feedTypeEnum.value.equals(value)) {
                return feedTypeEnum;
            }
        }
        return null;
    }
}