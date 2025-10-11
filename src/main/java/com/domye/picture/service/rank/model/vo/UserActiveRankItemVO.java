package com.domye.picture.service.rank.model.vo;

import com.domye.picture.service.user.model.vo.UserVO;
import lombok.Data;

@Data
public class UserActiveRankItemVO {
    /**
     * 排名
     */
    private Integer rank;

    /**
     * 评分
     */
    private double score;

    /**
     * 用户
     */
    private UserVO user;
}
