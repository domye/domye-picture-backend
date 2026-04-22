package com.domye.picture.model.entity.album;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 相册
 *
 * @TableName album
 */
@TableName(value = "album")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Album {

    /**
     * id（封面图片 id）
     */
    @TableId(type = IdType.INPUT)
    private Long id;


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
     * 图片数量
     */
    private Integer picCount;

    /**
     * 创建用户 id
     */
    private Long userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;


    /**
     * 空间 id（为空表示公共空间）
     */
    private Long spaceId;
}
