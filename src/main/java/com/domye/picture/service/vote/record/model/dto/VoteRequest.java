package com.domye.picture.service.vote.record.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VoteRequest implements Serializable {
    Long activityId;
    Long userId;
    Long optionId;
}
