package com.domye.picture.api.service.vote.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class VoteActivityAddRequest implements Serializable {
    private String title;
    private String description;

    private Date StartTime;
    private Date EndTime;
    private Integer maxVotesPerUser;
    private List<VoteOptionAddRequest> options; // 投票选项列表
}
