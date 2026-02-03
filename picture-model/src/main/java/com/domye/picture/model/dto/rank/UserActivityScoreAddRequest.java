package com.domye.picture.model.dto.rank;

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
