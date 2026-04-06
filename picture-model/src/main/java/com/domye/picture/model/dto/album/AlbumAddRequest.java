package com.domye.picture.model.dto.album;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AlbumAddRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 相册名称
     */
    private String name;

    /**
     * 相册简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（JSON 数组）
     */
    private String tags;

    /**
     * 封面图 url
     */
    private String coverUrl;

    /**
     * 空间 id（为空表示公共空间）
     */
    private Long spaceId;
}
