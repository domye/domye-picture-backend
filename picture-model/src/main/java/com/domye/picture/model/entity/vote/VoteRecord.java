package com.domye.picture.model.entity.vote;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 投票记录表
 * @TableName vote_records
 */
@TableName(value = "vote_records")
@Data
public class VoteRecord {
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
     * 选项ID
     */
    private Long optionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 客户端IP
     */
    private String clientIp;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     *
     */
    private Date voteTime;
}