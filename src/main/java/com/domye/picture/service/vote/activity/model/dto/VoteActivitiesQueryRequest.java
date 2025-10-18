package com.domye.picture.service.vote.activity.model.dto;

import com.domye.picture.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class VoteActivitiesQueryRequest extends PageRequest {
    private Long id;

    /**
     * 活动标题
     */
    private String title;

    /**
     * 创建人ID
     */
    private Long createUser;

    /**
     * 活动描述
     */
    private String description;

    /**
     * 开始时间
     */
    private Date startTime;

    /**
     * 结束时间
     */
    private Date endTime;

    /**
     * 状态：0-未开始，1-进行中 2-已结束 3-已暂停
     */
    private Integer status;

    private Long spaceId;
    /**
     * 搜索词（同时搜名称、简介等）
     */
    private String searchText;
}
