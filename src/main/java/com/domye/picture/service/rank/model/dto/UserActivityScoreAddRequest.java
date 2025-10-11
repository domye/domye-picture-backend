package com.domye.picture.service.rank.model.dto;

import lombok.Data;

@Data
public class UserActivityScoreAddRequest {
    /*
     * 访问页面增加活跃度
     */
    private String path;

    /*
     * 目标图片
     */
    private Long pictureId;


    /*
     * 是否上传图片
     */
    private Boolean uploadPicture;
}
