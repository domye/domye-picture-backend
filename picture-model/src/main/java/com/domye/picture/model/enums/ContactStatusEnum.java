package com.domye.picture.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum ContactStatusEnum implements Serializable {

    PENDING(0, "待确认"),
    ACCEPTED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private static final long serialVersionUID = 1L;

    private final Integer value;
    private final String text;

    ContactStatusEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值
     * @return 枚举值
     */
    public static ContactStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (ContactStatusEnum anEnum : ContactStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    /**
     * 根据 value 获取文本
     *
     * @param value 枚举值
     * @return 文本
     */
    public static String getTextByValue(Integer value) {
        ContactStatusEnum anEnum = getEnumByValue(value);
        return anEnum == null ? null : anEnum.getText();
    }

    /**
     * 获取所有枚举的文本列表
     *
     * @return 文本列表
     */
    public static List<String> getAllTexts() {
        return Arrays.stream(ContactStatusEnum.values())
                .map(ContactStatusEnum::getText)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有枚举的值列表
     *
     * @return 值列表
     */
    public static List<Integer> getAllValues() {
        return Arrays.stream(ContactStatusEnum.values())
                .map(ContactStatusEnum::getValue)
                .collect(Collectors.toList());
    }
}