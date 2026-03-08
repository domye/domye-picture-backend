package com.domye.picture.model.dto.rank;

import lombok.Data;

@Data
public class UserActivityScoreAddRequest {
    /**
     * 访问页面增加活跃度
     */
    private String path;

    /**
     * 目标图片
     */
    private Long pictureId;

    /**
     * 是否上传图片
     */
    private Boolean uploadPicture;

    /**
     * 是否评论图片
     */
    private Boolean commentPicture;

    /**
     * 是否点赞图片
     */
    private Boolean likePicture;

    /**
     * 是否收藏图片
     */
    private Boolean favoritePicture;

    /**
     * 是否分享图片
     */
    private Boolean sharePicture;
}
