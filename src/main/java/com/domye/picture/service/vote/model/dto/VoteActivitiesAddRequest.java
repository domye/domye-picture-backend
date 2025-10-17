package com.domye.picture.service.vote.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class VoteActivitiesAddRequest implements Serializable {
    private String title;
    private String description;

    private Date StartTime;
    private Date EndTime;
    private Integer maxVotesPerUser;
    private Long spaceId;
}
