package com.domye.picture.service.vote.activity.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 投票活动表
 * @TableName vote_activities
 */
@TableName(value = "vote_activities")
@Data
public class VoteActivities {
    /**
     *
     */
    @TableId(type = IdType.ASSIGN_ID)
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

    /**
     * 每用户最大投票数
     */
    private Integer maxVotesPerUser;

    /**
     * 总投票数
     */
    private Long totalVotes;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 关联空间
     */
    private Long spaceId;
}