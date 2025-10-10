package com.domye.picture.model.picture.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class PictureUploadRequest implements Serializable {
    private Long id;
    private Long spaceId;
    /**
     * 图片主色调
     */
    private String picColor;
}










































