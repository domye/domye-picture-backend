package com.domye.picture.model.vo.vote;

import lombok.Data;

@Data
public class VoteOptionVO {
    private Long id;                       // 选项 ID
    private String optionText;             // 选项文本
    private Long voteCount;                // 票数
}
