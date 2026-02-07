package com.domye.picture.model.dto.vote;

import lombok.Data;

import java.io.Serializable;

@Data
public class VoteActivityDeleteRequest implements Serializable {
    private Long activityId;
}
