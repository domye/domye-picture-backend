package com.domye.picture.service.vote.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class VoteActivitiesUpdateRequest implements Serializable {
    private Long id;
    private String title;
    private String description;

    private Date EndTime;
    private Integer maxVotesPerUser;
    private Integer status;
}
