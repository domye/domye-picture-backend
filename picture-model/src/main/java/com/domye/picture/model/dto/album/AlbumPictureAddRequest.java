package com.domye.picture.model.dto.album;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class AlbumPictureAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 相册 id
     */
    private Long albumId;

    /**
     * 图片 id 列表
     */
    private List<Long> pictureIds;
}
