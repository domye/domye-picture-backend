package com.domye.picture.service.vote.record.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class VoteEventRequest {
    Long activityId;
    Long userId;
    Long optionId;
    Integer increment;
    private Date voteTime;
}
