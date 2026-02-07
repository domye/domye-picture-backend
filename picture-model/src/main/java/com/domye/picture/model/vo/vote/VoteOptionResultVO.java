package com.domye.picture.model.vo.vote;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VoteOptionResultVO {
    private Long optionId;
    private String optionText;
    private Long voteCount;
    private BigDecimal voteRate;           // 投票占比（百分比）
}