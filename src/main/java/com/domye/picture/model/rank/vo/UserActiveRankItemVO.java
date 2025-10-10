package com.domye.picture.model.rank.vo;

import com.domye.picture.model.user.vo.UserVO;
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
