package com.domye.picture.service.vote.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class ActivityAddRequest implements Serializable {
    private String title;
    private String description;

    private Date StartTime;
    private Date EndTime;
    private Integer maxVotesPerUser;
    private Long spaceId;
    private List<OptionAddRequest> options; // 投票选项列表
}
