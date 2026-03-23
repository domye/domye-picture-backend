package com.domye.picture.model.vo.picture;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 个人主页作品列表项 VO
 * 用于个人主页展示，不包含用户信息（因为所有作品属于同一用户）
 */
@Data
public class PictureWorkVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 图片 id
     */
    private Long id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 分类
     */
    private String category;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片比例
     */
    private Double picScale;

    /**
     * 创建时间
     */
    private Date createTime;
}