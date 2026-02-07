package com.domye.picture.model.dto.vote;

import lombok.Data;

import java.io.Serializable;

@Data
public class VoteRequest implements Serializable {
    Long activityId;
    Long optionId;
    Long userId;
}
