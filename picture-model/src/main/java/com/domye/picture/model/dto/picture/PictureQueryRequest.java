package com.domye.picture.model.dto.picture;

import com.domye.picture.common.result.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;
    /**
     * 图片名称
     */
    private String name;
    /**
     * 简介
     */
    private String introduction;
    /**
     * 分类
     */
    private String category;
    /**
     * 标签
     */
    private List<String> tags;
    /**
     * 文件体积
     */
    private Long picSize;
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
     * 图片格式
     */
    private String picFormat;
    /**
     * 搜索词（同时搜名称、简介等）
     */
    private String searchText;
    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 状态：0-待审核; 1-通过; 2-拒绝
     */
    private Integer reviewStatus;

    /**
     * 审核信息
     */
    private String reviewMessage;

    /**
     * 审核人 id
     */
    private Long reviewerId;
    /**
     * 开始编辑时间
     */
    private Date startEditTime;

    /**
     * 结束编辑时间
     */
    private Date endEditTime;

    /**
     * 空间 id
     */
    private Long spaceId;


    /**
     * 相册类型过滤：true-只查询不属于相册 (0) 或相册主图 (2) 的图片；false-不过滤
     */
    private List<Integer> albumType;


}