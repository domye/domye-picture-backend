package com.domye.picture.service.vote.model.dto;

import lombok.Data;

@Data
public class VoteOptionsUpdateRequest {
    private Long optionId;
    /**
     * 活动ID
     */
    private Long activityId;

    /**
     * 选项内容
     */
    private String optionText;

}
