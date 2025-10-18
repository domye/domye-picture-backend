package com.domye.picture.service.vote.record.model.dto;

import lombok.Data;

@Data
public class VoteRequest {
    Long activityId;
    Long userId;
    Long optionId;
}
