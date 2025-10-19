package com.domye.picture.service.vote.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 投票选项表
 * @TableName vote_options
 */
@TableName(value = "vote_options")
@Data
public class VoteOption {
    /**
     *
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 选项内容
     */
    private String optionText;

    /**
     * 得票数
     */
    private Long voteCount;

    /**
     * 创建时间
     */
    private Date createTime;
}