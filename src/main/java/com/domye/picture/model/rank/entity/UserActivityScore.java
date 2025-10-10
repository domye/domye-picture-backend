package com.domye.picture.model.rank.entity;

import lombok.Data;

@Data
public class UserActivityScore {
    /**
     * 访问页面增加活跃度
     */
    private String path;

    /**
     * 目标图片
     */
    private Long pictureId;

    private Boolean uploadPicture;
}
