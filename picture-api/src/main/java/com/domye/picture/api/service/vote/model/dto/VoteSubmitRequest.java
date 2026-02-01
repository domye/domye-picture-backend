package com.domye.picture.api.service.vote.model.dto;

import lombok.Data;

@Data
public class VoteSubmitRequest {
    private Long activityId;               // 活动 ID
    private Long optionId;                 // 选项 ID
}
