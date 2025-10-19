package com.domye.picture.service.vote.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ActivityDetailVO {
    private Long id;                       // 活动 ID
    private String title;                  // 标题
    private String description;            // 描述
    private Date startTime;       // 开始时间
    private Date endTime;         // 结束时间
    private Integer status;                // 活动状态（1：进行中，0：未开始，2：已结束）
    private Integer maxVotesPerUser;       // 每人最大投票数
    private Long totalVotes;               // 活动总票数
    private List<OptionVO> options;        // 选项列表（含票数）
    private Boolean hasVoted;              // 当前用户是否已投票（前端展示用）
}