package com.domye.picture.service.vote.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VoteRequest implements Serializable {
    Long activityId;
    Long optionId;
    Long userId;
}
