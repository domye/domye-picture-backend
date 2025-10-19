package com.domye.picture.service.vote.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class VoteResultVO {
    private Long activityId;               // 活动 ID
    private Long totalVotes;               // 总票数
    private List<VoteOptionResultVO> options;  // 各选项结果
}