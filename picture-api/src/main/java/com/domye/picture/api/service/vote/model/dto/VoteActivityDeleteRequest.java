package com.domye.picture.api.service.vote.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class VoteActivityDeleteRequest implements Serializable {
    private Long activityId;
}
